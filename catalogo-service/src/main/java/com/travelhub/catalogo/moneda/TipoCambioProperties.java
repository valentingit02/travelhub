package com.travelhub.catalogo.moneda;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * url: API publica de tipos de cambio (Frankfurter, datos del BCE, sin API key).
 * respaldo: USD por unidad de cada moneda, se usa si la API no responde.
 */
@ConfigurationProperties(prefix = "travelhub.tipo-cambio")
public record TipoCambioProperties(String url, Duration vigencia, Map<String, BigDecimal> respaldo) { }
