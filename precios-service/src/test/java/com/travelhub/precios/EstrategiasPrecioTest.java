package com.travelhub.precios;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.precios.config.PreciosProperties;
import com.travelhub.precios.cotizacion.CotizacionRequest;
import com.travelhub.precios.cotizacion.CotizacionResponse;
import com.travelhub.precios.cotizacion.CotizadorService;
import com.travelhub.precios.estrategia.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias puras (sin levantar Spring): "hoy" fijo en 01/03/2027. */
class EstrategiasPrecioTest {

    private final LocalDate hoy = LocalDate.of(2027, 3, 1);
    private final Clock clock = Clock.fixed(hoy.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));
    private final PreciosProperties props = new PreciosProperties(Set.of(1, 2, 7, 12),
            new BigDecimal("1.30"), new BigDecimal("0.90"), 4, new BigDecimal("0.90"), new BigDecimal("0.93"));

    private CotizadorService cotizador;

    @BeforeEach
    void setUp() {
        cotizador = new CotizadorService(List.of(
                new TemporadaStrategy(props),
                new AnticipacionStrategy(clock),
                new OcupacionStrategy(),
                new GrupoStrategy(props)));
    }

    private ContextoCotizacion ctx(TipoProducto t, LocalDate inicio, int pax, double ocup) {
        return new ContextoCotizacion(t, inicio, inicio.plusDays(3), pax, ocup);
    }

    @Test
    void julioEsTemporadaAlta() {
        var e = new TemporadaStrategy(props);
        assertEquals(new BigDecimal("1.30"), e.factor(ctx(TipoProducto.HOTEL, LocalDate.of(2027, 7, 10), 2, 0)));
    }

    @Test
    void abrilEsTemporadaBaja() {
        var e = new TemporadaStrategy(props);
        assertEquals(new BigDecimal("0.90"), e.factor(ctx(TipoProducto.HOTEL, LocalDate.of(2027, 4, 10), 2, 0)));
    }

    @Test
    void compraConMasDe60DiasTieneDescuento() {
        var e = new AnticipacionStrategy(clock);
        assertEquals(new BigDecimal("0.85"), e.factor(ctx(TipoProducto.VUELO, hoy.plusDays(90), 1, 0)));
    }

    @Test
    void compraDeUltimoMomentoTieneRecargo() {
        var e = new AnticipacionStrategy(clock);
        assertEquals(new BigDecimal("1.20"), e.factor(ctx(TipoProducto.VUELO, hoy.plusDays(3), 1, 0)));
    }

    @Test
    void ocupacionAltaSubeElPrecioPeroNoAplicaAExcursiones() {
        var e = new OcupacionStrategy();
        assertEquals(new BigDecimal("1.25"), e.factor(ctx(TipoProducto.HOTEL, hoy.plusDays(10), 2, 0.9)));
        assertFalse(e.aplicaA(ctx(TipoProducto.EXCURSION, hoy.plusDays(10), 2, 0.9)));
    }

    @Test
    void grupoSoloAplicaAExcursionesDe4OMas() {
        var e = new GrupoStrategy(props);
        assertTrue(e.aplicaA(ctx(TipoProducto.EXCURSION, hoy.plusDays(10), 4, 0)));
        assertFalse(e.aplicaA(ctx(TipoProducto.EXCURSION, hoy.plusDays(10), 3, 0)));
        assertFalse(e.aplicaA(ctx(TipoProducto.HOTEL, hoy.plusDays(10), 6, 0)));
    }

    @Test
    void cotizaHotelEncadenandoFactores() {
        // Hotel 100 USD/noche, 3 noches en julio, 131 dias de anticipacion, ocupacion 60 %
        // 300 x 1.30 (alta) x 0.85 (anticipada) x 1.10 (ocupacion) = 364.65
        LocalDate inicio = LocalDate.of(2027, 7, 10);
        CotizacionResponse r = cotizador.cotizar(new CotizacionRequest(TipoProducto.HOTEL,
                new BigDecimal("100"), "USD", inicio, inicio.plusDays(3), 2, 0.6));

        assertEquals(3, r.unidades());
        assertEquals(new BigDecimal("300.00"), r.subtotal());
        assertEquals(3, r.factores().size());
        assertEquals(new BigDecimal("364.65"), r.precioFinal());
    }
}
