package com.travelhub.catalogo.busqueda;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@Tag(name = "Busqueda", description = "Vuelos (Duffel), hoteles y excursiones (Hotelbeds) y autos propios")
public class BusquedaController {

    private final BusquedaService service;

    public BusquedaController(BusquedaService service) {
        this.service = service;
    }

    @GetMapping("/buscar")
    @Operation(summary = "Busca todo lo necesario para armar un paquete")
    public BusquedaResponse buscar(@RequestParam String origen, @RequestParam String destino,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   @RequestParam(defaultValue = "2") int pax) {
        return service.buscar(origen, destino, desde, hasta, pax);
    }

    @GetMapping("/vuelos")
    @Operation(summary = "Busca vuelos de ida")
    public List<ProductoResponse> vuelos(@RequestParam String origen, @RequestParam String destino,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                         @RequestParam(defaultValue = "1") int pax) {
        return service.buscarVuelos(origen, destino, fecha, pax);
    }

    @GetMapping("/hoteles")
    @Operation(summary = "Busca hoteles (precio por noche)")
    public List<ProductoResponse> hoteles(@RequestParam String destino,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout,
                                          @RequestParam(defaultValue = "2") int pax) {
        return service.buscarHoteles(destino, checkin, checkout, pax);
    }

    @GetMapping("/excursiones")
    @Operation(summary = "Busca excursiones (precio por persona)")
    public List<ProductoResponse> excursiones(@RequestParam String destino,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.buscarExcursiones(destino, desde, hasta);
    }

    @GetMapping("/autos/disponibles")
    @Operation(summary = "Autos propios disponibles en el destino para esas fechas")
    public List<ProductoResponse> autosDisponibles(@RequestParam String destino,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.buscarAutos(destino, desde, hasta);
    }
}
