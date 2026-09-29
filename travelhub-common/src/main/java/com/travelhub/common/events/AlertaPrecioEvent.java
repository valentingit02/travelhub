package com.travelhub.common.events;

import java.math.BigDecimal;

public record AlertaPrecioEvent(String email, String nombre, String destino, String productoRef,
                                BigDecimal precioNuevo, BigDecimal precioObjetivo, String moneda) { }
