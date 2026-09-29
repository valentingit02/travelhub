package com.travelhub.precios.paquete;

import java.math.BigDecimal;

/** Patron Composite: un item suelto y un paquete se tratan igual. */
public interface Cotizable {
    BigDecimal total();
}
