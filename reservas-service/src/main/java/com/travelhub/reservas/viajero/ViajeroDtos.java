package com.travelhub.reservas.viajero;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;

public final class ViajeroDtos {

    private ViajeroDtos() { }

    /** Alta sin contrasena (version anterior). Se mantiene por compatibilidad: ver /api/auth/registro. */
    public record Request(@NotBlank String nombre, @NotBlank String apellido, @NotBlank @Email String email,
                          @NotBlank @Size(min = 6, max = 20) String documento,
                          @NotNull @Past LocalDate fechaNacimiento, String preferencias) { }

    /**
     * Datos editables del perfil. El email no se cambia: es el usuario de ingreso.
     * documento vacio = se conserva el actual (la respuesta solo lo muestra enmascarado).
     */
    public record PerfilRequest(@NotBlank @Size(max = 80) String nombre, @NotBlank @Size(max = 80) String apellido,
                                @Pattern(regexp = "^$|^\\S.{4,18}\\S$", message = "debe tener entre 6 y 20 caracteres") String documento,
                                @NotNull @Past LocalDate fechaNacimiento,
                                @Size(max = 200) String preferencias,
                                @Size(max = 30) @Pattern(regexp = "^$|^[+0-9 ()-]{6,30}$", message = "teléfono inválido") String telefono,
                                @Pattern(regexp = "^$|^[A-Za-z]{3}$", message = "debe ser un código IATA de 3 letras") String aeropuertoOrigen) { }

    public record CambioPasswordRequest(@NotBlank String actual,
                                        @NotBlank @Size(min = 8, max = 72) String nueva) { }

    /** El documento no se expone completo (privacidad) y el hash de la contrasena nunca sale del servicio. */
    public record Response(Long id, String nombre, String apellido, String email, String documentoEnmascarado,
                           LocalDate fechaNacimiento, String preferencias, String telefono,
                           String aeropuertoOrigen, Instant ultimoIngreso) {
        public static Response from(Viajero v) {
            String doc = v.getDocumento();
            String masc = "****" + doc.substring(Math.max(0, doc.length() - 3));
            return new Response(v.getId(), v.getNombre(), v.getApellido(), v.getEmail(), masc,
                    v.getFechaNacimiento(), v.getPreferencias(), v.getTelefono(), v.getAeropuertoOrigen(),
                    v.getUltimoIngreso());
        }
    }

    /** Vacio -> null, y el IATA en mayusculas. */
    static String limpio(String s, boolean mayusculas) {
        if (s == null || s.isBlank()) return null;
        return mayusculas ? s.trim().toUpperCase() : s.trim();
    }
}
