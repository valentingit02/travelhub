package com.travelhub.reservas.cliente;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.ExternalServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Map;

/** Cliente REST (sincronico) hacia catalogo-service. */
@Component
public class CatalogoClient {

    private final RestClient http;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient http) {
        this.http = http;
    }

    public record Titular(String nombre, String apellido, String email, LocalDate fechaNacimiento) { }

    private record Respuesta(TipoProducto tipo, String productoId, String refExterna) { }

    public String reservar(TipoProducto tipo, String productoId, LocalDate desde, LocalDate hasta,
                           int pasajeros, String reservaRef, Titular titular) {
        try {
            Respuesta r = http.post().uri("/api/catalogo/reservas-proveedor")
                    .body(Map.of("tipo", tipo, "productoId", productoId, "desde", desde.toString(),
                            "hasta", hasta.toString(), "pasajeros", pasajeros, "reservaRef", reservaRef,
                            "titular", titular))
                    .retrieve().body(Respuesta.class);
            return r.refExterna();
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException(tipo + " rechazado por catalogo (" + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new ExternalServiceException("catalogo-service no disponible: " + e.getMessage(), e);
        }
    }

    public void cancelar(TipoProducto tipo, String refExterna, String reservaRef) {
        try {
            http.delete().uri(u -> u.path("/api/catalogo/reservas-proveedor")
                            .queryParam("tipo", tipo).queryParam("ref", refExterna)
                            .queryParam("reservaRef", reservaRef).build())
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new ExternalServiceException("No se pudo cancelar " + tipo + " " + refExterna + ": " + e.getMessage(), e);
        }
    }
}
