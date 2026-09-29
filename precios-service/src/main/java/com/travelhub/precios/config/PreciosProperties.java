package com.travelhub.precios.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Set;

/** Configuracion externalizada: las reglas de precio se cambian en application.yml, no en el codigo. */
@ConfigurationProperties(prefix = "travelhub.precios")
public record PreciosProperties(
        Set<Integer> mesesTemporadaAlta,
        BigDecimal factorTemporadaAlta,
        BigDecimal factorTemporadaBaja,
        int pasajerosDescuentoGrupo,
        BigDecimal factorGrupo,
        BigDecimal factorPaquete) { }
