package com.travelhub.catalogo;

import com.travelhub.catalogo.huella.HuellaCarbono;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HuellaCarbonoTest {

    @Test
    void aeroparqueBarilocheRondaLos1300Km() {
        int km = HuellaCarbono.kilometros("AEP", "BRC").orElseThrow();
        assertTrue(km > 1200 && km < 1400, "km=" + km);
        assertTrue(HuellaCarbono.kgPorPasajero("AEP", "BRC").orElseThrow() > 100);
    }

    @Test
    void vueloLargoEmiteMasPeroConMenorFactor() {
        int corto = HuellaCarbono.kgPorPasajero("AEP", "MDZ").orElseThrow();
        int largo = HuellaCarbono.kgPorPasajero("EZE", "MAD").orElseThrow();
        assertTrue(largo > corto * 5);
    }

    @Test
    void aeropuertoDesconocidoNoEstima() {
        assertTrue(HuellaCarbono.kgPorPasajero("XXX", "BRC").isEmpty());
    }
}
