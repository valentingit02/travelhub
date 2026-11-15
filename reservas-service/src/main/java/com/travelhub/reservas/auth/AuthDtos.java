package com.travelhub.reservas.auth;

import com.travelhub.reservas.viajero.ViajeroDtos;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;

public final class AuthDtos {

    private AuthDtos() { }

    /** BCrypt usa como maximo 72 bytes: por eso el tope. */
    public record RegistroRequest(@NotBlank @Size(max = 80) String nombre, @NotBlank @Size(max = 80) String apellido,
                                  @NotBlank @Email @Size(max = 120) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @NotBlank @Size(min = 6, max = 20) String documento,
                                  @NotNull @Past LocalDate fechaNacimiento,
                                  @Size(max = 200) String preferencias) { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }

    /** Para cuentas creadas antes del login: se prueba que es el titular con el documento. */
    public record ActivarRequest(@NotBlank @Email String email, @NotBlank String documento,
                                 @NotBlank @Size(min = 8, max = 72) String password) { }

    public record SesionResponse(String token, Instant expiraEn, ViajeroDtos.Response viajero) { }
}
