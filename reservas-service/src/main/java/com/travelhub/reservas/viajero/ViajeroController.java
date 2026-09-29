package com.travelhub.reservas.viajero;

import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Servicio de Clientes del enunciado (aca: Viajeros). */
@RestController
@RequestMapping("/api/viajeros")
@Tag(name = "Viajeros")
public class ViajeroController {

    private final ViajeroRepository repo;

    public ViajeroController(ViajeroRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un viajero")
    public ViajeroDtos.Response crear(@Valid @RequestBody ViajeroDtos.Request r) {
        repo.findByEmailIgnoreCase(r.email()).ifPresent(v -> {
            throw new ConflictException("Ya existe un viajero con el email " + r.email());
        });
        Viajero v = repo.save(new Viajero(r.nombre(), r.apellido(), r.email().toLowerCase(), r.documento(),
                r.fechaNacimiento(), r.preferencias()));
        return ViajeroDtos.Response.from(v);
    }

    @GetMapping
    @Operation(summary = "Lista viajeros o busca por email")
    public List<ViajeroDtos.Response> listar(@RequestParam(required = false) String email) {
        if (email != null && !email.isBlank()) {
            return repo.findByEmailIgnoreCase(email).map(ViajeroDtos.Response::from).stream().toList();
        }
        return repo.findAll().stream().map(ViajeroDtos.Response::from).toList();
    }

    @GetMapping("/{id}")
    public ViajeroDtos.Response obtener(@PathVariable Long id) {
        return repo.findById(id).map(ViajeroDtos.Response::from)
                .orElseThrow(() -> new NotFoundException("No existe el viajero " + id));
    }
}
