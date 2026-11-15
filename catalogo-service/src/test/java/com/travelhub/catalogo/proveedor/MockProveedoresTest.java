package com.travelhub.catalogo.proveedor;

import com.travelhub.common.web.ExternalServiceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** El hotel de la demo de Saga tiene un id unico por destino y siempre falla al reservar. */
class MockProveedoresTest {

    private final MockProveedores mock = new MockProveedores();
    private final LocalDate in = LocalDate.of(2027, 7, 10);

    @Test
    void cadaOfertaTieneUnIdUnicoEntreDestinos() {
        Set<String> ids = new HashSet<>();
        for (String d : List.of("BRC", "MDZ", "IGR", "USH")) {
            mock.hoteles(d, in, in.plusDays(3), 2).forEach(o -> assertTrue(ids.add(o.id()), "id repetido: " + o.id()));
        }
        assertTrue(ids.contains("MOCK-HOTEL-BRC-FALLA"));
    }

    @Test
    void elHotelFallaSiempreRechazaLaReserva() {
        assertThrows(ExternalServiceException.class, () -> mock.reservar("MOCK-HOTEL-BRC-FALLA"));
        assertThrows(ExternalServiceException.class, () -> mock.reservar("MOCK-HOTEL-FALLA"));   // id anterior
    }

    @Test
    void unHotelNormalSeReserva() {
        assertTrue(mock.reservar("MOCK-HOTEL-BRC-0").startsWith("MOCKREF-"));
    }
}
