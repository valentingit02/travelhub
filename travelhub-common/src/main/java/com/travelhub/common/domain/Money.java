package com.travelhub.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Value object inmutable para montos. Evita sumar ARS con USD por error. */
public record Money(BigDecimal monto, String moneda) {

    public Money {
        Objects.requireNonNull(monto, "El monto es obligatorio");
        Objects.requireNonNull(moneda, "La moneda es obligatoria");
        if (monto.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        moneda = moneda.toUpperCase();
    }

    public static Money of(double monto, String moneda) {
        return new Money(BigDecimal.valueOf(monto), moneda);
    }

    public Money multiplicar(BigDecimal factor) {
        return new Money(monto.multiply(factor), moneda);
    }

    public Money sumar(Money otro) {
        if (!moneda.equals(otro.moneda())) {
            throw new IllegalArgumentException("No se pueden sumar " + moneda + " y " + otro.moneda());
        }
        return new Money(monto.add(otro.monto()), moneda);
    }
}
