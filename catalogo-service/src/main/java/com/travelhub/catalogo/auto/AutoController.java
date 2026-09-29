package com.travelhub.catalogo.auto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Capa de presentacion (REST) del inventario de autos. */
@RestController
@RequestMapping("/api/catalogo/autos")
@Tag(name = "Autos", description = "Inventario propio de autos de alquiler")
public class AutoController {

    private final AutoService service;

    public AutoController(AutoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista autos activos, opcionalmente filtrados por destino IATA")
    public List<AutoResponse> listar(@RequestParam(required = false) String destino) {
        return service.listar(destino);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un auto por id")
    public AutoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Da de alta un auto")
    public AutoResponse crear(@Valid @RequestBody AutoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica un auto (incluido su precio)")
    public AutoResponse actualizar(@PathVariable Long id, @Valid @RequestBody AutoRequest request) {
        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/aprobar")
    @Operation(summary = "Aprueba un auto que la IA marco como anomalo y lo vuelve a publicar")
    public AutoResponse aprobar(@PathVariable Long id) {
        return service.aprobarRevision(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Baja logica de un auto")
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
