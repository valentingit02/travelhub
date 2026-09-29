package com.travelhub.precios.estrategia;

import java.math.BigDecimal;

/**
 * Patron Strategy: cada regla de precio es una clase intercambiable.
 * Agregar una regla nueva = crear un @Component que implemente esta interfaz.
 * El cotizador las recibe todas por inyeccion y las encadena.
 */
public interface EstrategiaPrecio {

    String nombre();

    /** Factor multiplicador: 1.00 = sin cambio, 1.30 = +30 %, 0.90 = -10 %. */
    BigDecimal factor(ContextoCotizacion ctx);

    /** Motivo legible del factor, se devuelve en el desglose de la cotizacion. */
    String motivo(ContextoCotizacion ctx);

    default boolean aplicaA(ContextoCotizacion ctx) {
        return true;
    }
}
