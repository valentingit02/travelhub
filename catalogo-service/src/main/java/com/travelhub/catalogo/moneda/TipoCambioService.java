package com.travelhub.catalogo.moneda;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelhub.common.web.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Normaliza todos los precios a USD. Hotelbeds cotiza en EUR y Duffel en USD:
 * sin esto no se puede armar un paquete con productos de los dos proveedores.
 * Cachea la cotizacion (vigencia configurable) y, si la API falla, usa valores de respaldo.
 */
@Service
public class TipoCambioService {

    private static final Logger log = LoggerFactory.getLogger(TipoCambioService.class);
    public static final String MONEDA_BASE = "USD";

    public record Cotizacion(String moneda, BigDecimal usdPorUnidad, String fuente, Instant obtenida) { }

    private final TipoCambioProperties props;
    private final RestClient http;
    private final CircuitBreaker circuito;
    private final Map<String, Cotizacion> cache = new ConcurrentHashMap<>();

    public TipoCambioService(TipoCambioProperties props, RestClient.Builder builder, CircuitBreakerRegistry registry) {
        this.props = props;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofSeconds(5));
        this.http = builder.clone().baseUrl(props.url()).requestFactory(f).build();
        this.circuito = registry.circuitBreaker("tipoCambio");
    }

    public BigDecimal aUsd(BigDecimal monto, String moneda) {
        if (moneda == null || MONEDA_BASE.equalsIgnoreCase(moneda)) {
            return monto.setScale(2, RoundingMode.HALF_UP);
        }
        return monto.multiply(cotizacion(moneda).usdPorUnidad()).setScale(2, RoundingMode.HALF_UP);
    }

    public Cotizacion cotizacion(String moneda) {
        String m = moneda.toUpperCase();
        if (MONEDA_BASE.equals(m)) {
            return new Cotizacion(m, BigDecimal.ONE, "identidad", Instant.now());
        }
        Cotizacion enCache = cache.get(m);
        if (enCache != null && enCache.obtenida().plus(props.vigencia()).isAfter(Instant.now())) {
            return enCache;
        }
        try {
            JsonNode r = circuito.executeSupplier(() -> http.get()
                    .uri(u -> u.path("/latest")
                            .queryParam("base", m).queryParam("symbols", MONEDA_BASE)
                            .queryParam("from", m).queryParam("to", MONEDA_BASE)
                            .build())
                    .retrieve().body(JsonNode.class));
            BigDecimal tasa = new BigDecimal(r.path("rates").path(MONEDA_BASE).asText());
            Cotizacion nueva = new Cotizacion(m, tasa, "API " + r.path("date").asText("sin fecha"), Instant.now());
            cache.put(m, nueva);
            log.info("Tipo de cambio {} -> USD = {} ({})", m, tasa, nueva.fuente());
            return nueva;
        } catch (Exception e) {
            BigDecimal respaldo = props.respaldo() == null ? null : props.respaldo().get(m);
            if (respaldo == null) {
                throw new ExternalServiceException("No hay tipo de cambio disponible para " + m);
            }
            log.warn("API de tipo de cambio no disponible ({}), uso respaldo {} -> USD = {}", e.getMessage(), m, respaldo);
            Cotizacion c = new Cotizacion(m, respaldo, "respaldo configurado", Instant.now());
            cache.put(m, c);
            return c;
        }
    }

    public Map<String, Cotizacion> vigentes() {
        return Map.copyOf(cache);
    }
}
