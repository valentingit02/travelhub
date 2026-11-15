package com.travelhub.reservas.auth;

import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.UnauthorizedException;
import com.travelhub.reservas.seguridad.TokenService;
import com.travelhub.reservas.seguridad.UsuarioActual;
import com.travelhub.reservas.viajero.Viajero;
import com.travelhub.reservas.viajero.ViajeroDtos;
import com.travelhub.reservas.viajero.ViajeroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Registro, ingreso y alta de contrasena para cuentas anteriores al login. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    static final String CREDENCIALES_INVALIDAS = "Email o contrasena incorrectos.";
    static final String CUENTA_SIN_PASSWORD =
            "Tu cuenta es anterior al ingreso con contrasena. Crea tu contrasena confirmando tu documento.";

    private final ViajeroRepository viajeros;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    /** Si el email no existe igual se compara contra este hash: la respuesta tarda lo mismo y no revela que cuentas hay. */
    private final String hashFalso;

    public AuthService(ViajeroRepository viajeros, PasswordEncoder encoder, TokenService tokens) {
        this.viajeros = viajeros;
        this.encoder = encoder;
        this.tokens = tokens;
        this.hashFalso = encoder.encode("hash-para-igualar-tiempos");
    }

    public AuthDtos.SesionResponse registrar(AuthDtos.RegistroRequest r) {
        String email = normalizar(r.email());
        viajeros.findByEmailIgnoreCase(email).ifPresent(v -> {
            throw new ConflictException(v.tienePassword()
                    ? "Ya existe una cuenta con ese email. Ingresa con tu contrasena."
                    : "Ya existe una cuenta con ese email. Ingresa y crea tu contrasena confirmando tu documento.");
        });
        Viajero v = new Viajero(r.nombre().trim(), r.apellido().trim(), email, r.documento().trim(),
                r.fechaNacimiento(), r.preferencias());
        v.cambiarPassword(encoder.encode(r.password()));
        log.info("Cuenta creada para {}", UsuarioActual.enmascarar(email));
        return abrirSesion(v);
    }

    public AuthDtos.SesionResponse ingresar(AuthDtos.LoginRequest r) {
        String email = normalizar(r.email());
        Optional<Viajero> encontrado = viajeros.findByEmailIgnoreCase(email);
        if (encontrado.isEmpty()) {
            encoder.matches(r.password(), hashFalso);
            log.warn("Ingreso fallido (email inexistente): {}", UsuarioActual.enmascarar(email));
            throw new UnauthorizedException(CREDENCIALES_INVALIDAS);
        }
        Viajero v = encontrado.get();
        if (!v.tienePassword()) {
            throw new ConflictException(CUENTA_SIN_PASSWORD);
        }
        if (!encoder.matches(r.password(), v.getPasswordHash())) {
            log.warn("Ingreso fallido (contrasena incorrecta): {}", UsuarioActual.enmascarar(email));
            throw new UnauthorizedException(CREDENCIALES_INVALIDAS);
        }
        return abrirSesion(v);
    }

    public AuthDtos.SesionResponse activar(AuthDtos.ActivarRequest r) {
        String email = normalizar(r.email());
        Viajero v = viajeros.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Los datos no coinciden con ninguna cuenta."));
        if (v.tienePassword()) {
            throw new ConflictException("Tu cuenta ya tiene contrasena: ingresa normalmente.");
        }
        if (!v.getDocumento().trim().equalsIgnoreCase(r.documento().trim())) {
            log.warn("Activacion fallida (documento no coincide): {}", UsuarioActual.enmascarar(email));
            throw new UnauthorizedException("Los datos no coinciden con ninguna cuenta.");
        }
        v.cambiarPassword(encoder.encode(r.password()));
        log.info("Contrasena creada para cuenta existente {}", UsuarioActual.enmascarar(email));
        return abrirSesion(v);
    }

    /** La contrasena actual incorrecta es 400 (no 401) para que el frontend no cierre la sesion. */
    public void cambiarPassword(Long viajeroId, String actual, String nueva) {
        Viajero v = viajeros.findById(viajeroId).orElseThrow(() -> new UnauthorizedException("Tu sesion no es valida."));
        if (!v.tienePassword() || !encoder.matches(actual, v.getPasswordHash())) {
            throw new IllegalArgumentException("La contrasena actual no es correcta.");
        }
        v.cambiarPassword(encoder.encode(nueva));
        viajeros.save(v);
        log.info("Contrasena cambiada para {}", UsuarioActual.enmascarar(v.getEmail()));
    }

    private AuthDtos.SesionResponse abrirSesion(Viajero v) {
        v.registrarIngreso();
        Viajero guardado = viajeros.save(v);
        TokenService.Emitido t = tokens.emitir(guardado);
        return new AuthDtos.SesionResponse(t.token(), t.expiraEn(), t.roles(), ViajeroDtos.Response.from(guardado));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
