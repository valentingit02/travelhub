package com.travelhub.reservas.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClientResponseException;

/** Extrae el campo "mensaje" del ApiError que devuelven los otros servicios. */
final class Respuestas {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Respuestas() { }

    static String mensaje(RestClientResponseException e) {
        try {
            String m = MAPPER.readTree(e.getResponseBodyAsString()).path("mensaje").asText("");
            return m.isBlank() ? "HTTP " + e.getStatusCode().value() : m;
        } catch (Exception ex) {
            return "HTTP " + e.getStatusCode().value();
        }
    }
}
