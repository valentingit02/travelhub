package com.travelhub.precios.cotizacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.travelhub.precios.paquete.CotizacionPaqueteRequest;
import com.travelhub.precios.paquete.CotizacionPaqueteResponse;
import com.travelhub.precios.paquete.CotizadorPaqueteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/precios")
@Tag(name = "Precios", description = "Cotizacion dinamica de productos turisticos")
public class CotizacionController {

    private final CotizadorService service;
    private final CotizadorPaqueteService paqueteService;

    public CotizacionController(CotizadorService service, CotizadorPaqueteService paqueteService) {
        this.service = service;
        this.paqueteService = paqueteService;
    }

    @PostMapping("/cotizar")
    @Operation(summary = "Cotiza un producto y devuelve el desglose de factores aplicados")
    public CotizacionResponse cotizar(@Valid @RequestBody CotizacionRequest request) {
        return service.cotizar(request);
    }

    @PostMapping("/cotizar-paquete")
    @Operation(summary = "Cotiza un paquete (Composite) con descuento si combina 2 o mas tipos de producto")
    public CotizacionPaqueteResponse cotizarPaquete(@Valid @RequestBody CotizacionPaqueteRequest request) {
        return paqueteService.cotizar(request);
    }
}
