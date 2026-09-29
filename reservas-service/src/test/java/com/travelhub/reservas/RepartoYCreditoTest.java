package com.travelhub.reservas;

import com.travelhub.reservas.compartida.Reparto;
import com.travelhub.reservas.credito.CreditoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepartoYCreditoTest {

    @Test
    void repartoSumaExactoYElOrganizadorAbsorbeElResto() {
        List<BigDecimal> partes = Reparto.partes(new BigDecimal("100.00"), 3);
        assertEquals(new BigDecimal("33.34"), partes.get(0));
        assertEquals(new BigDecimal("33.33"), partes.get(1));
        assertEquals(new BigDecimal("100.00"), partes.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void creditoProporcionalALaBajaSobreLoPagado() {
        // Pago 500 por un auto de base 100/dia; el precio base baja a 80: se devuelve el 20 % de lo pagado
        BigDecimal c = CreditoService.compensacion(new BigDecimal("500"), new BigDecimal("100"),
                new BigDecimal("100"), new BigDecimal("80"));
        assertEquals(new BigDecimal("100.00"), c);
    }

    @Test
    void segundaBajaSoloCompensaLaDiferenciaNueva() {
        BigDecimal c = CreditoService.compensacion(new BigDecimal("500"), new BigDecimal("100"),
                new BigDecimal("80"), new BigDecimal("70"));
        assertEquals(new BigDecimal("50.00"), c);
    }

    @Test
    void subidaOBajaSospechosaNoCompensa() {
        assertEquals(0, CreditoService.compensacion(new BigDecimal("500"), new BigDecimal("100"),
                new BigDecimal("100"), new BigDecimal("120")).signum());
        assertEquals(0, CreditoService.compensacion(new BigDecimal("500"), new BigDecimal("100"),
                new BigDecimal("100"), new BigDecimal("1")).signum());
    }
}
