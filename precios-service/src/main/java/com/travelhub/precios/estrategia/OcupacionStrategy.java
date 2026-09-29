package com.travelhub.precios.estrategia;

import com.travelhub.common.domain.TipoProducto;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A mayor ocupacion (asientos, habitaciones o autos ya vendidos), mayor precio. */
@Component
@Order(3)
public class OcupacionStrategy implements EstrategiaPrecio {

    @Override
    public String nombre() { return "OCUPACION"; }

    @Override
    public boolean aplicaA(ContextoCotizacion ctx) {
        return ctx.tipo() != TipoProducto.EXCURSION;
    }

    @Override
    public BigDecimal factor(ContextoCotizacion ctx) {
        if (ctx.ocupacion() > 0.80) return new BigDecimal("1.25");
        if (ctx.ocupacion() > 0.50) return new BigDecimal("1.10");
        return BigDecimal.ONE;
    }

    @Override
    public String motivo(ContextoCotizacion ctx) {
        return "Ocupacion " + Math.round(ctx.ocupacion() * 100) + " %";
    }
}
