package com.travelhub.catalogo.busqueda;

import com.travelhub.catalogo.auto.Auto;
import com.travelhub.catalogo.auto.AutoService;
import com.travelhub.catalogo.moneda.TipoCambioService;
import com.travelhub.catalogo.oferta.OfertaService;
import com.travelhub.catalogo.proveedor.*;
import com.travelhub.common.domain.Money;
import com.travelhub.common.domain.ProductoTuristico;
import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.factory.ProductoFactory;
import com.travelhub.common.util.ValidadorFechas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Busqueda unificada: consulta los cuatro proveedores en paralelo, normaliza todo a USD,
 * registra cada oferta (precio verificable al reservar) y oculta las bloqueadas por la IA.
 */
@Service
public class BusquedaService {

    private static final Logger log = LoggerFactory.getLogger(BusquedaService.class);

    private final ProveedorVuelos vuelos;
    private final ProveedorHoteles hoteles;
    private final ProveedorExcursiones excursiones;
    private final AutoService autos;
    private final TipoCambioService tipoCambio;
    private final OfertaService ofertas;

    public BusquedaService(ProveedorVuelos vuelos, ProveedorHoteles hoteles, ProveedorExcursiones excursiones,
                           AutoService autos, TipoCambioService tipoCambio, OfertaService ofertas) {
        this.vuelos = vuelos;
        this.hoteles = hoteles;
        this.excursiones = excursiones;
        this.autos = autos;
        this.tipoCambio = tipoCambio;
        this.ofertas = ofertas;
    }

    public BusquedaResponse buscar(String origen, String destino, LocalDate desde, LocalDate hasta, int pax) {
        ValidadorFechas.validarRango(desde, hasta);
        String o = origen.toUpperCase();
        String d = destino.toUpperCase();

        var fVuelos = CompletableFuture.supplyAsync(() -> buscarVuelos(o, d, desde, pax));
        var fHoteles = CompletableFuture.supplyAsync(() -> buscarHoteles(d, desde, hasta, pax));
        var fExc = CompletableFuture.supplyAsync(() -> buscarExcursiones(d, desde, hasta));
        List<ProductoResponse> listaAutos = buscarAutos(d, desde, hasta);

        return new BusquedaResponse(fVuelos.join(), fHoteles.join(), fExc.join(), listaAutos);
    }

    public List<ProductoResponse> buscarVuelos(String origen, String destino, LocalDate fecha, int pax) {
        return publicar(vuelos.buscar(origen.toUpperCase(), destino.toUpperCase(), fecha, pax));
    }

    public List<ProductoResponse> buscarHoteles(String destino, LocalDate in, LocalDate out, int pax) {
        ValidadorFechas.validarRango(in, out);
        return publicar(hoteles.buscar(destino.toUpperCase(), in, out, pax));
    }

    public List<ProductoResponse> buscarExcursiones(String destino, LocalDate desde, LocalDate hasta) {
        return publicar(excursiones.buscar(destino.toUpperCase(), desde, hasta));
    }

    /** Los autos son inventario propio: su precio se verifica directo contra la tabla auto. */
    public List<ProductoResponse> buscarAutos(String destino, LocalDate desde, LocalDate hasta) {
        double ocupacion = autos.ocupacion(destino, desde, hasta);
        List<OfertaProveedor> lista = autos.disponibles(destino, desde, hasta).stream()
                .map(a -> aOferta(a, ocupacion)).toList();
        return mapear(normalizar(lista));
    }

    private List<ProductoResponse> publicar(List<OfertaProveedor> crudas) {
        return mapear(ofertas.publicar(normalizar(crudas)));
    }

    /** Convierte cada precio a USD y guarda el original en el detalle para mostrarlo. */
    private List<OfertaProveedor> normalizar(List<OfertaProveedor> crudas) {
        List<OfertaProveedor> out = new ArrayList<>();
        for (OfertaProveedor o : crudas) {
            if (TipoCambioService.MONEDA_BASE.equalsIgnoreCase(o.moneda())) {
                out.add(o);
                continue;
            }
            try {
                BigDecimal usd = tipoCambio.aUsd(o.precioBase(), o.moneda());
                Map<String, String> detalle = new LinkedHashMap<>(o.detalle());
                detalle.put("precioOriginal", o.moneda() + " " + o.precioBase().toPlainString());
                out.add(new OfertaProveedor(o.id(), o.tipo(), o.proveedor(), o.nombre(), o.destino(), usd,
                        TipoCambioService.MONEDA_BASE, o.unidad(), o.ocupacion(), detalle));
            } catch (RuntimeException e) {
                log.warn("Oferta {} descartada: no se pudo convertir {} a USD ({})", o.id(), o.moneda(), e.getMessage());
            }
        }
        return out;
    }

    private OfertaProveedor aOferta(Auto a, double ocupacion) {
        return new OfertaProveedor("AUTO-" + a.getId(), TipoProducto.AUTO, "TRAVELHUB",
                a.getMarca() + " " + a.getModelo() + " (" + a.getCategoria() + ")", a.getDestinoIata(),
                a.getPrecioBaseDia(), a.getMoneda(), "dias", ocupacion,
                Map.of("plazas", String.valueOf(a.getPlazas()),
                        "transmision", a.isTransmisionAutomatica() ? "Automatica" : "Manual"));
    }

    private List<ProductoResponse> mapear(List<OfertaProveedor> lista) {
        return lista.stream().map(o -> {
            ProductoTuristico p = ProductoFactory.crear(o.tipo(), o.id(), o.destino(),
                    new Money(o.precioBase(), o.moneda()));
            return ProductoResponse.from(p, o);
        }).toList();
    }
}
