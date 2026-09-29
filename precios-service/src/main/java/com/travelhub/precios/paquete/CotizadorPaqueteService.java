package com.travelhub.precios.paquete;

import com.travelhub.precios.config.PreciosProperties;
import com.travelhub.precios.cotizacion.CotizacionRequest;
import com.travelhub.precios.cotizacion.CotizacionResponse;
import com.travelhub.precios.cotizacion.CotizadorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CotizadorPaqueteService {

    private final CotizadorService cotizador;
    private final PreciosProperties props;

    public CotizadorPaqueteService(CotizadorService cotizador, PreciosProperties props) {
        this.cotizador = cotizador;
        this.props = props;
    }

    public CotizacionPaqueteResponse cotizar(CotizacionPaqueteRequest request) {
        String moneda = request.items().getFirst().moneda().toUpperCase();
        if (request.items().stream().anyMatch(i -> !i.moneda().equalsIgnoreCase(moneda))) {
            throw new IllegalArgumentException("Todos los items del paquete deben estar en la misma moneda (" + moneda + ")");
        }
        List<CotizacionResponse> cotizaciones = new ArrayList<>();
        List<Cotizable> hojas = new ArrayList<>();
        for (CotizacionRequest item : request.items()) {
            CotizacionResponse c = cotizador.cotizar(item);
            cotizaciones.add(c);
            hojas.add(new ItemCotizado(c));
        }
        long tipos = request.items().stream().map(CotizacionRequest::tipoProducto).distinct().count();
        BigDecimal factor = tipos >= 2 ? props.factorPaquete() : BigDecimal.ONE;
        String motivo = tipos >= 2 ? "Descuento por paquete (" + tipos + " tipos de producto)" : "Sin descuento";

        PaqueteCotizado paquete = new PaqueteCotizado(hojas, factor);
        return new CotizacionPaqueteResponse(cotizaciones, paquete.subtotal(), factor, motivo, paquete.total(), moneda);
    }
}
