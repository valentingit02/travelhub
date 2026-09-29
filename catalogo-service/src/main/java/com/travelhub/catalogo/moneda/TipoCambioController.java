package com.travelhub.catalogo.moneda;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/catalogo/tipo-cambio")
@Tag(name = "Tipo de cambio", description = "Todos los precios se normalizan a USD")
public class TipoCambioController {

    private final TipoCambioService service;

    public TipoCambioController(TipoCambioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Cotizaciones en cache (moneda -> USD)")
    public Map<String, TipoCambioService.Cotizacion> vigentes() {
        return service.vigentes();
    }

    @GetMapping("/{moneda}")
    @Operation(summary = "Cotizacion de una moneda a USD (consulta la API si no esta en cache)")
    public TipoCambioService.Cotizacion cotizacion(@PathVariable String moneda) {
        return service.cotizacion(moneda);
    }
}
