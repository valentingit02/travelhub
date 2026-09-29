package com.travelhub.common;

import com.travelhub.common.domain.*;
import com.travelhub.common.factory.ProductoFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductoFactoryTest {

    @Test
    void creaElTipoConcretoCorrecto() {
        ProductoTuristico p = ProductoFactory.crear(TipoProducto.HOTEL, "H1", "BRC", Money.of(100, "usd"));
        assertInstanceOf(Hotel.class, p);
        assertEquals(TipoProducto.HOTEL, p.getTipo());
        assertEquals("USD", p.getPrecioBase().moneda());
    }

    @Test
    void noPermiteSumarMonedasDistintas() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(10, "USD").sumar(Money.of(10, "ARS")));
    }
}
