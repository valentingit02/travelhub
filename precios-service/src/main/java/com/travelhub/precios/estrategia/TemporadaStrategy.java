package com.travelhub.precios.estrategia;

import com.travelhub.precios.config.PreciosProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class TemporadaStrategy implements EstrategiaPrecio {

    private final PreciosProperties props;

    public TemporadaStrategy(PreciosProperties props) {
        this.props = props;
    }

    @Override
    public String nombre() { return "TEMPORADA"; }

    private boolean esAlta(ContextoCotizacion ctx) {
        return props.mesesTemporadaAlta().contains(ctx.fechaInicio().getMonthValue());
    }

    @Override
    public BigDecimal factor(ContextoCotizacion ctx) {
        return esAlta(ctx) ? props.factorTemporadaAlta() : props.factorTemporadaBaja();
    }

    @Override
    public String motivo(ContextoCotizacion ctx) {
        return esAlta(ctx) ? "Temporada alta" : "Temporada baja";
    }
}
