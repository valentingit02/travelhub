package com.travelhub.common.factory;

import com.travelhub.common.domain.*;

/**
 * Patron Factory: crea el producto concreto segun el tipo.
 * Los adapters de APIs externas (Duffel, Hotelbeds) y el inventario propio
 * usan esta fabrica, asi el resto del sistema no conoce las clases concretas.
 */
public final class ProductoFactory {

    private ProductoFactory() { }

    public static ProductoTuristico crear(TipoProducto tipo, String id, String destino, Money precioBase) {
        return switch (tipo) {
            case VUELO -> new Vuelo(id, destino, precioBase);
            case HOTEL -> new Hotel(id, destino, precioBase);
            case EXCURSION -> new Excursion(id, destino, precioBase);
            case AUTO -> new AutoAlquiler(id, destino, precioBase);
        };
    }
}
