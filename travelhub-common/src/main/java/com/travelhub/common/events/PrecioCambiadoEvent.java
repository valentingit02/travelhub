package com.travelhub.common.events;

import java.math.BigDecimal;

/** productoRef ej: "AUTO-7". precioAnterior es null cuando el producto es nuevo. */
public record PrecioCambiadoEvent(String productoRef, String tipoProducto, String destino,
                                  BigDecimal precioAnterior, BigDecimal precioNuevo, String moneda) { }
