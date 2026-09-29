package com.travelhub.precios.soap;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.precios.cotizacion.CotizacionRequest;
import com.travelhub.precios.cotizacion.CotizacionResponse;
import com.travelhub.precios.cotizacion.CotizadorService;
import com.travelhub.precios.paquete.CotizacionPaqueteRequest;
import com.travelhub.precios.paquete.CotizacionPaqueteResponse;
import com.travelhub.precios.paquete.CotizadorPaqueteService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Servicio SOAP (canal B2B para agencias asociadas). Reutiliza exactamente la misma
 * logica de negocio que el REST: solo cambia la capa de presentacion.
 * WSDL: http://localhost:8082/ws/cotizacion.wsdl
 */
@Endpoint
public class CotizacionEndpoint {

    public static final String NS = "http://travelhub.com/precios/soap";

    private final CotizadorService cotizador;
    private final CotizadorPaqueteService paquetes;

    public CotizacionEndpoint(CotizadorService cotizador, CotizadorPaqueteService paquetes) {
        this.cotizador = cotizador;
        this.paquetes = paquetes;
    }

    @PayloadRoot(namespace = NS, localPart = "CotizarProductoRequest")
    @ResponsePayload
    public CotizarProductoResponse cotizarProducto(@RequestPayload CotizarProductoRequest request) {
        try {
            CotizarProductoResponse resp = new CotizarProductoResponse();
            resp.cotizacion = aSoap(cotizador.cotizar(aDominio(request.item)));
            return resp;
        } catch (IllegalArgumentException | DateTimeParseException | NullPointerException e) {
            throw new CotizacionInvalidaException("Cotizacion invalida: " + e.getMessage());
        }
    }

    @PayloadRoot(namespace = NS, localPart = "CotizarPaqueteRequest")
    @ResponsePayload
    public CotizarPaqueteResponse cotizarPaquete(@RequestPayload CotizarPaqueteRequest request) {
        try {
            CotizacionPaqueteResponse r = paquetes.cotizar(new CotizacionPaqueteRequest(
                    request.item.stream().map(this::aDominio).toList()));
            CotizarPaqueteResponse resp = new CotizarPaqueteResponse();
            r.items().forEach(c -> resp.cotizacion.add(aSoap(c)));
            resp.subtotal = r.subtotal();
            resp.factorPaquete = r.factorPaquete();
            resp.motivoDescuento = r.motivoDescuento();
            resp.total = r.total();
            resp.moneda = r.moneda();
            return resp;
        } catch (IllegalArgumentException | DateTimeParseException | NullPointerException
                 | java.util.NoSuchElementException e) {
            throw new CotizacionInvalidaException("Paquete invalido: " + e.getMessage());
        }
    }

    private CotizacionRequest aDominio(SoapItem i) {
        if (i.pasajeros < 1 || i.pasajeros > 9) throw new IllegalArgumentException("pasajeros debe estar entre 1 y 9");
        if (i.ocupacion < 0 || i.ocupacion > 1) throw new IllegalArgumentException("ocupacion debe estar entre 0 y 1");
        if (i.precioBase == null || i.precioBase.signum() <= 0) throw new IllegalArgumentException("precioBase debe ser positivo");
        return new CotizacionRequest(TipoProducto.valueOf(i.tipoProducto), i.precioBase, i.moneda,
                LocalDate.parse(i.fechaInicio), LocalDate.parse(i.fechaFin), i.pasajeros, i.ocupacion);
    }

    private SoapCotizacion aSoap(CotizacionResponse c) {
        SoapCotizacion s = new SoapCotizacion();
        s.tipoProducto = c.tipoProducto().name();
        s.precioBase = c.precioBase();
        s.unidades = c.unidades();
        s.unidad = c.unidad();
        s.subtotal = c.subtotal();
        c.factores().forEach(f -> {
            SoapFactor sf = new SoapFactor();
            sf.estrategia = f.estrategia();
            sf.factor = f.factor();
            sf.motivo = f.motivo();
            s.factor.add(sf);
        });
        s.precioFinal = c.precioFinal();
        s.moneda = c.moneda();
        return s;
    }
}
