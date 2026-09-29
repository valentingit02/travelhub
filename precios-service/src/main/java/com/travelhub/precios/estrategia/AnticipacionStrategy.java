package com.travelhub.precios.estrategia;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@Order(2)
public class AnticipacionStrategy implements EstrategiaPrecio {

    private final Clock clock;

    public AnticipacionStrategy(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String nombre() { return "ANTICIPACION"; }

    long dias(ContextoCotizacion ctx) {
        return ChronoUnit.DAYS.between(LocalDate.now(clock), ctx.fechaInicio());
    }

    @Override
    public BigDecimal factor(ContextoCotizacion ctx) {
        long d = dias(ctx);
        if (d >= 60) return new BigDecimal("0.85");
        if (d >= 30) return new BigDecimal("0.95");
        if (d < 7) return new BigDecimal("1.20");
        return BigDecimal.ONE;
    }

    @Override
    public String motivo(ContextoCotizacion ctx) {
        long d = dias(ctx);
        if (d >= 60) return "Compra anticipada (" + d + " dias)";
        if (d >= 30) return "Anticipacion media (" + d + " dias)";
        if (d < 7) return "Ultimo momento (" + d + " dias)";
        return "Anticipacion normal (" + d + " dias)";
    }
}
