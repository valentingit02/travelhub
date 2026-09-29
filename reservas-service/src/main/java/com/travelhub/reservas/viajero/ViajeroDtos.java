package com.travelhub.reservas.viajero;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class ViajeroDtos {

    private ViajeroDtos() { }

    public record Request(@NotBlank String nombre, @NotBlank String apellido, @NotBlank @Email String email,
                          @NotBlank @Size(min = 6, max = 20) String documento,
                          @NotNull @Past LocalDate fechaNacimiento, String preferencias) { }

    /** El documento no se expone completo (privacidad). */
    public record Response(Long id, String nombre, String apellido, String email, String documentoEnmascarado,
                           LocalDate fechaNacimiento, String preferencias) {
        public static Response from(Viajero v) {
            String doc = v.getDocumento();
            String masc = "****" + doc.substring(Math.max(0, doc.length() - 3));
            return new Response(v.getId(), v.getNombre(), v.getApellido(), v.getEmail(), masc,
                    v.getFechaNacimiento(), v.getPreferencias());
        }
    }
}
