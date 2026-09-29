package com.travelhub.reservas.cliente;

import com.travelhub.common.domain.TipoProducto;
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
import java.util.List;

/** Cliente REST hacia precios-service (cotizacion de paquete), con circuit breaker "precios". */
@Component
public class PreciosClient {

    private final RestClient http;
    private final CircuitBreaker circuito;

    public PreciosClient(@Qualifier("preciosRestClient") RestClient http, CircuitBreakerRegistry registry) {
        this.http = http;
        this.circuito = registry.circuitBreaker("precios");
    }

    public record Item(TipoProducto tipoProducto, BigDecimal precioBase, String moneda, LocalDate fechaInicio,
                       LocalDate fechaFin, Integer pasajeros, Double ocupacion) { }

    public record Cotizacion(TipoProducto tipoProducto, BigDecimal precioFinal, String moneda) { }

    public record Paquete(List<Cotizacion> items, BigDecimal subtotal, BigDecimal factorPaquete,
                          String motivoDescuento, BigDecimal total, String moneda) { }

    private record PaqueteRequest(List<Item> items) { }

    public Paquete cotizarPaquete(List<Item> items) {
        try {
            return circuito.executeSupplier(() -> http.post().uri("/api/precios/cotizar-paquete")
                    .body(new PaqueteRequest(items)).retrieve().body(Paquete.class));
        } catch (CallNotPermittedException e) {
            throw new ExternalServiceException(
                    "precios-service no disponible temporalmente (circuit breaker abierto). Proba en unos segundos.");
        } catch (RestClientResponseException e) {
            throw new IllegalArgumentException("No se pudo cotizar: " + Respuestas.mensaje(e));
        } catch (RestClientException e) {
            throw new ExternalServiceException("precios-service no disponible: " + e.getMessage(), e);
        }
    }
}
