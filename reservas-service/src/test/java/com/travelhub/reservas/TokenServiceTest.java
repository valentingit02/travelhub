package com.travelhub.reservas;

import com.travelhub.reservas.seguridad.SeguridadConfig;
import com.travelhub.reservas.seguridad.TokenService;
import com.travelhub.reservas.viajero.Viajero;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** El token se firma, se valida, y se rechaza si lo adulteran, vence o lo firmo otra clave. */
class TokenServiceTest {

    static final String CLAVE = "clave-de-test-con-mas-de-32-caracteres-ok";

    @Test
    void emiteUnTokenQueElDecoderAcepta() throws Exception {
        Beans b = beans(CLAVE);
        TokenService tokens = new TokenService(b.encoder(), Duration.ofHours(1));

        TokenService.Emitido t = tokens.emitir(viajero(7L));
        Jwt jwt = b.decoder().decode(t.token());

        assertEquals("7", jwt.getSubject());
        assertEquals("ana@test.com", jwt.getClaimAsString("email"));
        assertEquals(SeguridadConfig.EMISOR, jwt.getClaimAsString("iss"));
        assertTrue(t.expiraEn().isAfter(java.time.Instant.now()));
    }

    @Test
    void rechazaUnTokenAdulterado() throws Exception {
        Beans b = beans(CLAVE);
        String token = new TokenService(b.encoder(), Duration.ofHours(1)).emitir(viajero(7L)).token();
        String[] partes = token.split("\\.");
        // Cambio el payload (otro "sub") y dejo la firma original
        String otroPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"iss\":\"travelhub-reservas\",\"exp\":4102444800}".getBytes());
        assertThrows(JwtException.class, () -> b.decoder().decode(partes[0] + "." + otroPayload + "." + partes[2]));
    }

    @Test
    void rechazaUnTokenVencido() throws Exception {
        Beans b = beans(CLAVE);
        // Emitido hace 2 horas, vencio hace 5 minutos (el validador tolera 60 s de diferencia de reloj)
        java.time.Instant ahora = java.time.Instant.now();
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer(SeguridadConfig.EMISOR).subject("7")
                .issuedAt(ahora.minus(Duration.ofHours(2))).expiresAt(ahora.minus(Duration.ofMinutes(5))).build();
        var header = org.springframework.security.oauth2.jwt.JwsHeader
                .with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        String token = b.encoder().encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        assertThrows(JwtException.class, () -> b.decoder().decode(token));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() throws Exception {
        String token = new TokenService(beans("otra-clave-distinta-tambien-de-32-caracteres").encoder(), Duration.ofHours(1))
                .emitir(viajero(7L)).token();
        assertThrows(JwtException.class, () -> beans(CLAVE).decoder().decode(token));
    }

    @Test
    void exigeUnaClaveDeAlMenos32Caracteres() {
        Exception e = assertThrows(Exception.class, () -> beans("corta"));
        assertTrue(rootMessage(e).contains("32"));
    }

    // ------------------------------------------------------------------ helpers

    record Beans(org.springframework.security.oauth2.jwt.JwtEncoder encoder, JwtDecoder decoder) { }

    /** Usa los mismos @Bean de SeguridadConfig (son package-private: se invocan por reflexion). */
    static Beans beans(String clave) throws Exception {
        SeguridadConfig config = new SeguridadConfig();
        SecretKey key = (SecretKey) invocar(config, "jwtKey", String.class, clave);
        var encoder = (org.springframework.security.oauth2.jwt.JwtEncoder) invocar(config, "jwtEncoder", SecretKey.class, key);
        var decoder = (JwtDecoder) invocar(config, "jwtDecoder", SecretKey.class, key);
        return new Beans(encoder, decoder);
    }

    private static Object invocar(Object target, String metodo, Class<?> tipo, Object arg) throws Exception {
        Method m = SeguridadConfig.class.getDeclaredMethod(metodo, tipo);
        m.setAccessible(true);
        return m.invoke(target, arg);
    }

    static Viajero viajero(Long id) {
        Viajero v = new Viajero("Ana", "Perez", "ana@test.com", "30111222", LocalDate.of(1995, 5, 10), "aventura");
        ReflectionTestUtils.setField(v, "id", id);
        return v;
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return String.valueOf(t.getMessage());
    }
}
