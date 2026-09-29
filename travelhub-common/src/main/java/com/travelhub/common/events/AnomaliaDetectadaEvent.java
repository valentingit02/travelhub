package com.travelhub.common.events;

import java.math.BigDecimal;

/** Lo publica ia-service (Python). */
public record AnomaliaDetectadaEvent(String productoRef, String tipoProducto, String destino,
                                     BigDecimal precio, String moneda, Double score, String motivo) { }
