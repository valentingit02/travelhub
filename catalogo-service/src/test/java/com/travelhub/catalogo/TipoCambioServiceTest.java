package com.travelhub.catalogo;

import com.travelhub.catalogo.moneda.TipoCambioProperties;
import com.travelhub.catalogo.moneda.TipoCambioService;
import com.travelhub.common.web.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Si la API de tipo de cambio no responde, se usa el valor de respaldo configurado. */
class TipoCambioServiceTest {

    private final TipoCambioService service = new TipoCambioService(
            new TipoCambioProperties("http://127.0.0.1:9", Duration.ofHours(6), Map.of("EUR", new BigDecimal("1.10"))),
            RestClient.builder(), CircuitBreakerRegistry.ofDefaults());

    @Test
    void usdNoSeConvierte() {
        assertEquals(new BigDecimal("150.00"), service.aUsd(new BigDecimal("150"), "USD"));
    }

    @Test
    void eurUsaRespaldoSiLaApiNoResponde() {
        assertEquals(new BigDecimal("110.00"), service.aUsd(new BigDecimal("100"), "EUR"));
        assertEquals("respaldo configurado", service.cotizacion("EUR").fuente());
    }

    @Test
    void monedaSinRespaldoFalla() {
        assertThrows(ExternalServiceException.class, () -> service.aUsd(new BigDecimal("100"), "JPY"));
    }
}
