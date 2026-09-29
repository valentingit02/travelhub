package com.travelhub.reservas.cliente;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.ExternalServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cliente REST hacia precios-service (cotizacion de paquete). */
@Component
public class PreciosClient {

    private final RestClient http;

    public PreciosClient(@Qualifier("preciosRestClient") RestClient http) {
        this.http = http;
    }

    public record Item(TipoProducto tipoProducto, BigDecimal precioBase, String moneda, LocalDate fechaInicio,
                       LocalDate fechaFin, Integer pasajeros, Double ocupacion) { }

    public record Cotizacion(TipoProducto tipoProducto, BigDecimal precioFinal, String moneda) { }

    public record Paquete(List<Cotizacion> items, BigDecimal subtotal, BigDecimal factorPaquete,
                          String motivoDescuento, BigDecimal total, String moneda) { }

    private record PaqueteRequest(List<Item> items) { }

    public Paquete cotizarPaquete(List<Item> items) {
        try {
            return http.post().uri("/api/precios/cotizar-paquete").body(new PaqueteRequest(items))
                    .retrieve().body(Paquete.class);
        } catch (RestClientException e) {
            throw new ExternalServiceException("precios-service no pudo cotizar: " + e.getMessage(), e);
        }
    }
}
