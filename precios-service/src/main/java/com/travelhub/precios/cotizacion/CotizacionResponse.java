package com.travelhub.precios.cotizacion;

import com.travelhub.common.domain.TipoProducto;

import java.math.BigDecimal;
import java.util.List;

public record CotizacionResponse(
        TipoProducto tipoProducto,
        BigDecimal precioBase,
        long unidades,
        String unidad,
        BigDecimal subtotal,
        List<FactorAplicado> factores,
        BigDecimal precioFinal,
        String moneda) {

    public record FactorAplicado(String estrategia, BigDecimal factor, String motivo) { }
}
