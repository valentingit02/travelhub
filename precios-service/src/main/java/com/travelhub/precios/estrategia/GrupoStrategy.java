package com.travelhub.precios.estrategia;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.precios.config.PreciosProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Descuento por grupo, solo para excursiones. */
@Component
@Order(4)
public class GrupoStrategy implements EstrategiaPrecio {

    private final PreciosProperties props;

    public GrupoStrategy(PreciosProperties props) {
        this.props = props;
    }

    @Override
    public String nombre() { return "GRUPO"; }

    @Override
    public boolean aplicaA(ContextoCotizacion ctx) {
        return ctx.tipo() == TipoProducto.EXCURSION && ctx.pasajeros() >= props.pasajerosDescuentoGrupo();
    }

    @Override
    public BigDecimal factor(ContextoCotizacion ctx) {
        return props.factorGrupo();
    }

    @Override
    public String motivo(ContextoCotizacion ctx) {
        return "Descuento grupal (" + ctx.pasajeros() + " personas)";
    }
}
