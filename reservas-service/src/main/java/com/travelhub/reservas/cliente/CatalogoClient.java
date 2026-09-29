package com.travelhub.reservas.cliente;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cliente REST (sincronico) hacia catalogo-service, protegido con el circuit breaker "catalogo".
 * Solo cuentan como falla los errores de red (servicio caido o timeout), no los rechazos de negocio.
 */
@Component
public class CatalogoClient {

    private final RestClient http;
    private final CircuitBreaker circuito;

    public CatalogoClient(@Qualifier("catalogoRestClient") RestClient http, CircuitBreakerRegistry registry) {
        this.http = http;
        this.circuito = registry.circuitBreaker("catalogo");
    }

    public record Titular(String nombre, String apellido, String email, LocalDate fechaNacimiento) { }

    /** Oferta con precio garantizado por el catalogo (siempre en USD). */
    public record Oferta(String id, TipoProducto tipo, String proveedor, String nombre, String destino,
                         BigDecimal precio, String moneda, double ocupacion) { }

    private record Respuesta(TipoProducto tipo, String productoId, String refExterna) { }

    public Oferta verificar(String productoId, LocalDate desde, LocalDate hasta) {
        try {
            return protegido(() -> http.get()
                    .uri(u -> u.path("/api/catalogo/ofertas")
                            .queryParam("id", "{id}")
                            .queryParam("desde", desde.toString())
                            .queryParam("hasta", hasta.toString())
                            .build(productoId))
                    .retrieve().body(Oferta.class));
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404 || status == 409) throw new ConflictException(Respuestas.mensaje(e));
            throw new ExternalServiceException("catalogo-service respondio " + status + ": " + Respuestas.mensaje(e));
        } catch (RestClientException e) {
            throw new ExternalServiceException("catalogo-service no disponible: " + e.getMessage(), e);
        }
    }

    public String reservar(TipoProducto tipo, String productoId, LocalDate desde, LocalDate hasta,
                           int pasajeros, String reservaRef, Titular titular) {
        try {
            Respuesta r = protegido(() -> http.post().uri("/api/catalogo/reservas-proveedor")
                    .body(Map.of("tipo", tipo, "productoId", productoId, "desde", desde.toString(),
                            "hasta", hasta.toString(), "pasajeros", pasajeros, "reservaRef", reservaRef,
                            "titular", titular))
                    .retrieve().body(Respuesta.class));
            return r.refExterna();
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException(tipo + " rechazado por el proveedor: " + Respuestas.mensaje(e));
        } catch (RestClientException e) {
            throw new ExternalServiceException("catalogo-service no disponible: " + e.getMessage(), e);
        }
    }

    public void cancelar(TipoProducto tipo, String refExterna, String reservaRef) {
        try {
            protegido(() -> http.delete().uri(u -> u.path("/api/catalogo/reservas-proveedor")
                            .queryParam("tipo", tipo)
                            .queryParam("ref", "{ref}")
                            .queryParam("reservaRef", reservaRef)
                            .build(refExterna))
                    .retrieve().toBodilessEntity());
        } catch (RestClientException e) {
            throw new ExternalServiceException("No se pudo cancelar " + tipo + " " + refExterna + ": " + e.getMessage(), e);
        }
    }

    private <T> T protegido(Supplier<T> llamada) {
        try {
            return circuito.executeSupplier(llamada);
        } catch (CallNotPermittedException e) {
            throw new ExternalServiceException(
                    "catalogo-service no disponible temporalmente (circuit breaker abierto). Proba en unos segundos.");
        }
    }
}
