package com.travelhub.precios.paquete;

import com.travelhub.precios.cotizacion.CotizacionResponse;

import java.math.BigDecimal;

/** Hoja del Composite: la cotizacion de un producto individual. */
public record ItemCotizado(CotizacionResponse cotizacion) implements Cotizable {
    @Override
    public BigDecimal total() {
        return cotizacion.precioFinal();
    }
}
