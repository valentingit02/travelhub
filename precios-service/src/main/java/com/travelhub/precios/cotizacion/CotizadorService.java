package com.travelhub.precios.cotizacion;

import com.travelhub.common.domain.Money;
import com.travelhub.common.util.ValidadorFechas;
import com.travelhub.precios.estrategia.ContextoCotizacion;
import com.travelhub.precios.estrategia.EstrategiaPrecio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica de negocio de la cotizacion.
 * Precio final = precio base x unidades x factor1 x factor2 x ... (Strategies encadenadas).
 * Este mismo servicio se expone por REST ahora y por SOAP en la semana 2.
 */
@Service
public class CotizadorService {

    private static final Logger log = LoggerFactory.getLogger(CotizadorService.class);
    private final List<EstrategiaPrecio> estrategias;

    public CotizadorService(List<EstrategiaPrecio> estrategias) {
        this.estrategias = estrategias; // Spring inyecta todas, ordenadas por @Order
    }

    public CotizacionResponse cotizar(CotizacionRequest r) {
        ValidadorFechas.validarRango(r.fechaInicio(), r.fechaFin());

        ContextoCotizacion ctx = new ContextoCotizacion(r.tipoProducto(), r.fechaInicio(), r.fechaFin(),
                r.pasajeros(), r.ocupacion());

        long unidades = calcularUnidades(r);
        Money subtotal = new Money(r.precioBase(), r.moneda()).multiplicar(BigDecimal.valueOf(unidades));

        List<CotizacionResponse.FactorAplicado> aplicados = new ArrayList<>();
        Money total = subtotal;
        for (EstrategiaPrecio e : estrategias) {
            if (!e.aplicaA(ctx)) continue;
            BigDecimal f = e.factor(ctx);
            total = total.multiplicar(f);
            aplicados.add(new CotizacionResponse.FactorAplicado(e.nombre(), f, e.motivo(ctx)));
        }

        log.info("Cotizacion {}: subtotal={} final={} {}", r.tipoProducto(), subtotal.monto(),
                total.monto(), total.moneda());

        return new CotizacionResponse(r.tipoProducto(), r.precioBase(), unidades, unidad(r),
                subtotal.monto(), aplicados, total.monto(), total.moneda());
    }

    private long calcularUnidades(CotizacionRequest r) {
        long dias = Math.max(1, ChronoUnit.DAYS.between(r.fechaInicio(), r.fechaFin()));
        return switch (r.tipoProducto()) {
            case HOTEL, AUTO -> dias;                 // noches / dias de alquiler
            case VUELO, EXCURSION -> r.pasajeros();   // por persona
        };
    }

    private String unidad(CotizacionRequest r) {
        return switch (r.tipoProducto()) {
            case HOTEL -> "noches";
            case AUTO -> "dias";
            case VUELO, EXCURSION -> "pasajeros";
        };
    }
}
