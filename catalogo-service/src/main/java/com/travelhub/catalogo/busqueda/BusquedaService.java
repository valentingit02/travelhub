package com.travelhub.catalogo.busqueda;

import com.travelhub.catalogo.auto.Auto;
import com.travelhub.catalogo.auto.AutoService;
import com.travelhub.catalogo.proveedor.*;
import com.travelhub.common.domain.Money;
import com.travelhub.common.domain.ProductoTuristico;
import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.factory.ProductoFactory;
import com.travelhub.common.util.ValidadorFechas;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Busqueda unificada: consulta los cuatro proveedores en paralelo y normaliza el resultado.
 * Usa ProductoFactory para construir el producto de dominio sin conocer las clases concretas.
 */
@Service
public class BusquedaService {

    private final ProveedorVuelos vuelos;
    private final ProveedorHoteles hoteles;
    private final ProveedorExcursiones excursiones;
    private final AutoService autos;

    public BusquedaService(ProveedorVuelos vuelos, ProveedorHoteles hoteles,
                           ProveedorExcursiones excursiones, AutoService autos) {
        this.vuelos = vuelos;
        this.hoteles = hoteles;
        this.excursiones = excursiones;
        this.autos = autos;
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
        return mapear(vuelos.buscar(origen.toUpperCase(), destino.toUpperCase(), fecha, pax));
    }

    public List<ProductoResponse> buscarHoteles(String destino, LocalDate in, LocalDate out, int pax) {
        ValidadorFechas.validarRango(in, out);
        return mapear(hoteles.buscar(destino.toUpperCase(), in, out, pax));
    }

    public List<ProductoResponse> buscarExcursiones(String destino, LocalDate desde, LocalDate hasta) {
        return mapear(excursiones.buscar(destino.toUpperCase(), desde, hasta));
    }

    public List<ProductoResponse> buscarAutos(String destino, LocalDate desde, LocalDate hasta) {
        double ocupacion = autos.ocupacion(destino, desde, hasta);
        List<OfertaProveedor> ofertas = autos.disponibles(destino, desde, hasta).stream()
                .map(a -> aOferta(a, ocupacion)).toList();
        return mapear(ofertas);
    }

    private OfertaProveedor aOferta(Auto a, double ocupacion) {
        return new OfertaProveedor("AUTO-" + a.getId(), TipoProducto.AUTO, "TRAVELHUB",
                a.getMarca() + " " + a.getModelo() + " (" + a.getCategoria() + ")", a.getDestinoIata(),
                a.getPrecioBaseDia(), a.getMoneda(), "dias", ocupacion,
                Map.of("plazas", String.valueOf(a.getPlazas()),
                        "transmision", a.isTransmisionAutomatica() ? "Automatica" : "Manual"));
    }

    private List<ProductoResponse> mapear(List<OfertaProveedor> ofertas) {
        return ofertas.stream().map(o -> {
            ProductoTuristico p = ProductoFactory.crear(o.tipo(), o.id(), o.destino(),
                    new Money(o.precioBase(), o.moneda()));
            return ProductoResponse.from(p, o);
        }).toList();
    }
}
