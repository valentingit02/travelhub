package com.travelhub.precios.paquete;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Nodo compuesto del Composite: suma sus items y aplica el descuento por paquete. */
public record PaqueteCotizado(List<Cotizable> items, BigDecimal factorPaquete) implements Cotizable {

    public BigDecimal subtotal() {
        return items.stream().map(Cotizable::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal total() {
        return subtotal().multiply(factorPaquete).setScale(2, RoundingMode.HALF_UP);
    }
}
