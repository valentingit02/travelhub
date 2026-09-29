package com.travelhub.catalogo.oferta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/catalogo/ofertas")
@Tag(name = "Ofertas", description = "Precio verificado por el servidor para reservar")
public class OfertaController {

    private final OfertaService service;

    public OfertaController(OfertaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Verifica una oferta: precio en USD, disponibilidad y vigencia (409 si no se puede reservar)")
    public OfertaVerificada verificar(@RequestParam String id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.verificar(id, desde, hasta);
    }

    @GetMapping("/bloqueadas")
    @Operation(summary = "Ofertas bloqueadas por la IA (precio anomalo)")
    public List<OfertaPublicada> bloqueadas() {
        return service.bloqueadas();
    }

    @PostMapping("/aprobar")
    @Operation(summary = "Aprueba manualmente una oferta bloqueada")
    public OfertaPublicada aprobar(@RequestParam String id) {
        return service.aprobar(id);
    }
}
