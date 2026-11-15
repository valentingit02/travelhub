package com.travelhub.reservas;

import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.UnauthorizedException;
import com.travelhub.reservas.auth.AuthDtos;
import com.travelhub.reservas.auth.AuthService;
import com.travelhub.reservas.seguridad.TokenService;
import com.travelhub.reservas.viajero.Viajero;
import com.travelhub.reservas.viajero.ViajeroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reglas del login: hash BCrypt, mensajes que no revelan cuentas, y alta de contrasena para cuentas viejas. */
class AuthServiceTest {

    private ViajeroRepository repo;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);   // costo bajo: el test es rapido
    private AuthService service;

    @BeforeEach
    void setUp() throws Exception {
        repo = mock(ViajeroRepository.class);
        when(repo.save(any(Viajero.class))).thenAnswer(i -> {
            Viajero v = i.getArgument(0);
            if (v.getId() == null) ReflectionTestUtils.setField(v, "id", 1L);
            return v;
        });
        TokenService tokens = new TokenService(TokenServiceTest.beans(TokenServiceTest.CLAVE).encoder(), Duration.ofHours(1));
        service = new AuthService(repo, encoder, tokens);
    }

    @Test
    void registroGuardaLaContrasenaHasheadaYAbreSesion() {
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.empty());

        AuthDtos.SesionResponse s = service.registrar(new AuthDtos.RegistroRequest("Ana", "Perez", " Ana@Test.com ",
                "secreta123", "30111222", LocalDate.of(1995, 5, 10), "aventura"));

        assertNotNull(s.token());
        assertEquals("ana@test.com", s.viajero().email());
        verify(repo).save(argThat(v -> v.tienePassword()
                && !v.getPasswordHash().contains("secreta123")
                && encoder.matches("secreta123", v.getPasswordHash())
                && v.getUltimoIngreso() != null));
    }

    @Test
    void registroConEmailExistenteEsConflicto() {
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(conPassword("otra1234")));
        assertThrows(ConflictException.class, () -> service.registrar(new AuthDtos.RegistroRequest("Ana", "Perez",
                "ana@test.com", "secreta123", "30111222", LocalDate.of(1995, 5, 10), null)));
        verify(repo, never()).save(any());
    }

    @Test
    void loginCorrectoDevuelveToken() {
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(conPassword("secreta123")));
        AuthDtos.SesionResponse s = service.ingresar(new AuthDtos.LoginRequest("ana@test.com", "secreta123"));
        assertNotNull(s.token());
        assertNotNull(s.viajero().ultimoIngreso());
    }

    @Test
    void contrasenaIncorrectaYEmailInexistenteDanElMismoError() {
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(conPassword("secreta123")));
        when(repo.findByEmailIgnoreCase("nadie@test.com")).thenReturn(Optional.empty());

        var mala = assertThrows(UnauthorizedException.class,
                () -> service.ingresar(new AuthDtos.LoginRequest("ana@test.com", "otra-cosa")));
        var inexistente = assertThrows(UnauthorizedException.class,
                () -> service.ingresar(new AuthDtos.LoginRequest("nadie@test.com", "secreta123")));
        assertEquals(mala.getMessage(), inexistente.getMessage());
    }

    @Test
    void cuentaAnteriorAlLoginPideCrearContrasena() {
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(sinPassword()));
        assertThrows(ConflictException.class, () -> service.ingresar(new AuthDtos.LoginRequest("ana@test.com", "x")));
    }

    @Test
    void activarConElDocumentoCorrectoCreaLaContrasena() {
        Viajero viejo = sinPassword();
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(viejo));

        AuthDtos.SesionResponse s = service.activar(new AuthDtos.ActivarRequest("ana@test.com", " 30111222 ", "nueva1234"));

        assertNotNull(s.token());
        assertTrue(encoder.matches("nueva1234", viejo.getPasswordHash()));
    }

    @Test
    void activarConOtroDocumentoNoCambiaNada() {
        Viajero viejo = sinPassword();
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(viejo));
        assertThrows(UnauthorizedException.class,
                () -> service.activar(new AuthDtos.ActivarRequest("ana@test.com", "99999999", "nueva1234")));
        assertFalse(viejo.tienePassword());
    }

    @Test
    void activarUnaCuentaQueYaTieneContrasenaEsConflicto() {
        when(repo.findByEmailIgnoreCase("ana@test.com")).thenReturn(Optional.of(conPassword("secreta123")));
        assertThrows(ConflictException.class,
                () -> service.activar(new AuthDtos.ActivarRequest("ana@test.com", "30111222", "nueva1234")));
    }

    @Test
    void cambiarPasswordExigeLaActual() {
        Viajero v = conPassword("secreta123");
        when(repo.findById(1L)).thenReturn(Optional.of(v));

        assertThrows(IllegalArgumentException.class, () -> service.cambiarPassword(1L, "incorrecta", "nueva1234"));
        service.cambiarPassword(1L, "secreta123", "nueva1234");

        assertTrue(encoder.matches("nueva1234", v.getPasswordHash()));
    }

    // ------------------------------------------------------------------ helpers

    private Viajero sinPassword() {
        return TokenServiceTest.viajero(1L);
    }

    private Viajero conPassword(String password) {
        Viajero v = sinPassword();
        v.cambiarPassword(encoder.encode(password));
        return v;
    }
}
