package com.travelhub.reservas;

import com.travelhub.common.web.ForbiddenException;
import com.travelhub.reservas.auth.AuthController;
import com.travelhub.reservas.auth.AuthService;
import com.travelhub.reservas.compartida.CompartidaController;
import com.travelhub.reservas.reserva.ReservaController;
import com.travelhub.reservas.reserva.ReservaDtos;
import com.travelhub.reservas.reserva.ReservaFacade;
import com.travelhub.reservas.seguridad.SeguridadConfig;
import com.travelhub.reservas.seguridad.TokenService;
import com.travelhub.reservas.viajero.ViajeroController;
import com.travelhub.reservas.viajero.ViajeroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seguridad de punta a punta en la capa HTTP (con tokens reales firmados por TokenService):
 * sin sesion -> 401, datos de otro viajero -> 403, rutas publicas siguen abiertas.
 */
@WebMvcTest(controllers = {ReservaController.class, ViajeroController.class, AuthController.class, CompartidaController.class})
@Import({SeguridadConfig.class, TokenService.class})
class SeguridadWebTest {

    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;

    @MockBean ReservaFacade facade;
    @MockBean ViajeroRepository viajeros;
    @MockBean AuthService auth;

    private String bearer(long viajeroId) {
        return "Bearer " + tokens.emitir(TokenServiceTest.viajero(viajeroId)).token();
    }

    private static final String RESERVA_JSON = """
            {"viajeroId": %s, "destino": "BRC", "desde": "2099-07-10", "hasta": "2099-07-15", "pasajeros": 2,
             "items": [{"tipo": "HOTEL", "productoId": "MOCK-HOTEL-1"}]}""";

    @Test
    void sinTokenEs401ConElFormatoDeErrorDeSiempre() throws Exception {
        mvc.perform(get("/api/reservas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").exists());
        verifyNoInteractions(facade);
    }

    @Test
    void tokenInvalidoEs401() throws Exception {
        mvc.perform(get("/api/reservas").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("sesión")));
    }

    @Test
    void conTokenListaSoloLasReservasPropias() throws Exception {
        when(facade.listar(1L)).thenReturn(List.of());
        mvc.perform(get("/api/reservas").header("Authorization", bearer(1)))
                .andExpect(status().isOk());
        verify(facade).listar(1L);
    }

    @Test
    void pedirLasReservasDeOtroViajeroEs403() throws Exception {
        mvc.perform(get("/api/reservas").param("viajeroId", "2").header("Authorization", bearer(1)))
                .andExpect(status().isForbidden());
        verify(facade, never()).listar(any());
    }

    @Test
    void reservarANombreDeOtroEs403() throws Exception {
        mvc.perform(post("/api/reservas").header("Authorization", bearer(1))
                        .contentType(MediaType.APPLICATION_JSON).content(RESERVA_JSON.formatted("2")))
                .andExpect(status().isForbidden());
        verify(facade, never()).reservarPaquete(any(), any());
    }

    @Test
    void reservarSinViajeroIdUsaElDeLaSesion() throws Exception {
        mvc.perform(post("/api/reservas").header("Authorization", bearer(1))
                        .contentType(MediaType.APPLICATION_JSON).content(RESERVA_JSON.formatted("null")))
                .andExpect(status().isCreated());
        verify(facade).reservarPaquete(argThat((ReservaDtos.Request r) -> r.viajeroId().equals(1L)), isNull());
    }

    @Test
    void cancelarUnaReservaAjenaEs403YNoCancelaNada() throws Exception {
        doThrow(new ForbiddenException("ajena")).when(facade).exigirDuenio(5L, 1L);
        mvc.perform(delete("/api/reservas/5").header("Authorization", bearer(1)))
                .andExpect(status().isForbidden());
        verify(facade, never()).cancelar(any());
    }

    @Test
    void elPerfilEsElDeLaSesion() throws Exception {
        when(viajeros.findById(1L)).thenReturn(Optional.of(TokenServiceTest.viajero(1L)));
        mvc.perform(get("/api/viajeros/me").header("Authorization", bearer(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.documentoEnmascarado").value("****222"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void verElPerfilDeOtroPorIdEs403() throws Exception {
        mvc.perform(get("/api/viajeros/2").header("Authorization", bearer(1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elLinkDeViajeCompartidoSigueSiendoPublicoAunqueLleguePorUnTokenVencido() throws Exception {
        mvc.perform(get("/api/reservas/compartidas/abc").header("Authorization", "Bearer vencido"))
                .andExpect(status().isOk());
        verify(facade).vista("abc");
    }

    @Test
    void loginEsPublico() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@test.com\",\"password\":\"secreta123\"}"))
                .andExpect(status().isOk());
        verify(auth).ingresar(any());
    }

    @Test
    void registroValidaLaLongitudDeLaContrasena() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ana","apellido":"Perez","email":"ana@test.com","password":"corta",
                         "documento":"30111222","fechaNacimiento":"1995-05-10"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.startsWith("password")));
        verifyNoInteractions(auth);
    }
}
