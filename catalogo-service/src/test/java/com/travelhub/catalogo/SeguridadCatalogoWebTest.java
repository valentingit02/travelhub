package com.travelhub.catalogo;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.travelhub.catalogo.auto.AutoController;
import com.travelhub.catalogo.auto.AutoService;
import com.travelhub.catalogo.busqueda.BusquedaController;
import com.travelhub.catalogo.busqueda.BusquedaService;
import com.travelhub.catalogo.oferta.OfertaController;
import com.travelhub.catalogo.oferta.OfertaService;
import com.travelhub.catalogo.seguridad.SeguridadConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seguridad del catalogo con tokens firmados igual que los de reservas-service:
 * cambiar precios y aprobar productos exige el rol ADMIN; buscar sigue siendo publico.
 */
@WebMvcTest(controllers = {AutoController.class, OfertaController.class, BusquedaController.class})
@Import(SeguridadConfig.class)
class SeguridadCatalogoWebTest {

    /** Es la clave por defecto de application.yml (la misma que usa reservas-service). */
    static final String CLAVE = "travelhub-dev-clave-solo-para-desarrollo-local-0001";

    @Autowired MockMvc mvc;
    @MockBean AutoService autos;
    @MockBean OfertaService ofertas;
    @MockBean BusquedaService busqueda;

    private static String token(String clave, List<String> roles) {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("travelhub-reservas").subject("1")
                .issuedAt(ahora).expiresAt(ahora.plusSeconds(3600)).claim("roles", roles).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static final String ADMIN = token(CLAVE, List.of("ADMIN"));
    private static final String VIAJERO = token(CLAVE, List.of());

    private static final String AUTO_JSON = """
            {"destinoIata":"BRC","categoria":"SUV","marca":"Toyota","modelo":"SW4","plazas":7,
             "transmisionAutomatica":true,"precioBaseDia":1,"moneda":"USD"}""";

    @Test
    void cambiarElPrecioSinTokenEs401() throws Exception {
        mvc.perform(put("/api/catalogo/autos/2").contentType(MediaType.APPLICATION_JSON).content(AUTO_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
        verifyNoInteractions(autos);
    }

    @Test
    void unViajeroComunNoPuedeCambiarPrecios() throws Exception {
        mvc.perform(put("/api/catalogo/autos/2").header("Authorization", VIAJERO)
                        .contentType(MediaType.APPLICATION_JSON).content(AUTO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ADMIN")));
        verifyNoInteractions(autos);
    }

    @Test
    void unAdminPuedeCambiarPrecios() throws Exception {
        mvc.perform(put("/api/catalogo/autos/2").header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(AUTO_JSON))
                .andExpect(status().isOk());
        verify(autos).actualizar(eq(2L), any());
    }

    @Test
    void unTokenFirmadoConOtraClaveEs401AunqueDigaAdmin() throws Exception {
        String falso = token("otra-clave-que-no-es-la-del-sistema-0002", List.of("ADMIN"));
        mvc.perform(put("/api/catalogo/autos/2").header("Authorization", falso)
                        .contentType(MediaType.APPLICATION_JSON).content(AUTO_JSON))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(autos);
    }

    @Test
    void altaBajaYAprobacionDeAutosSonDeAdmin() throws Exception {
        mvc.perform(post("/api/catalogo/autos").header("Authorization", VIAJERO)
                .contentType(MediaType.APPLICATION_JSON).content(AUTO_JSON)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/catalogo/autos/2").header("Authorization", VIAJERO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/catalogo/autos/2/aprobar").header("Authorization", VIAJERO)).andExpect(status().isForbidden());
        mvc.perform(post("/api/catalogo/autos/2/aprobar").header("Authorization", ADMIN)).andExpect(status().isOk());
        verify(autos).aprobarRevision(2L);
        verify(autos, never()).crear(any());
        verify(autos, never()).desactivar(any());
    }

    @Test
    void aprobarYListarOfertasBloqueadasEsDeAdmin() throws Exception {
        mvc.perform(get("/api/catalogo/ofertas/bloqueadas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/catalogo/ofertas/aprobar").param("id", "H1").header("Authorization", VIAJERO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/catalogo/ofertas/aprobar").param("id", "H1").header("Authorization", ADMIN))
                .andExpect(status().isOk());
        verify(ofertas).aprobar("H1");
    }

    @Test
    void buscarYVerAutosSiguenSiendoPublicos() throws Exception {
        mvc.perform(get("/api/catalogo/autos")).andExpect(status().isOk());
        mvc.perform(get("/api/catalogo/autos/2")).andExpect(status().isOk());
        mvc.perform(get("/api/catalogo/buscar").param("origen", "AEP").param("destino", "BRC")
                        .param("desde", "2027-07-10").param("hasta", "2027-07-15").param("pax", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void unTokenVencidoNoRompeUnaBusqueda() throws Exception {
        // El frontend manda el token en todas las llamadas: en las rutas publicas se ignora
        mvc.perform(get("/api/catalogo/buscar").header("Authorization", "Bearer token-vencido-o-basura")
                        .param("origen", "AEP").param("destino", "BRC")
                        .param("desde", "2027-07-10").param("hasta", "2027-07-15").param("pax", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void verificarUnaOfertaSigueAbiertoParaReservas() throws Exception {
        mvc.perform(get("/api/catalogo/ofertas").param("id", "H1").param("desde", "2027-07-10").param("hasta", "2027-07-15"))
                .andExpect(status().isOk());
        verify(ofertas).verificar(eq("H1"), any(), any());
    }
}
