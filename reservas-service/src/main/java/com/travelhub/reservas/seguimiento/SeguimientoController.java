package com.travelhub.reservas.seguimiento;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.NotFoundException;
import com.travelhub.reservas.viajero.ViajeroRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/seguimientos")
@Tag(name = "Alertas de precio")
public class SeguimientoController {

    private final SeguimientoRepository repo;
    private final ViajeroRepository viajeros;

    public SeguimientoController(SeguimientoRepository repo, ViajeroRepository viajeros) {
        this.repo = repo;
        this.viajeros = viajeros;
    }

    public record Request(@NotNull Long viajeroId, @NotBlank @Size(min = 3, max = 3) String destino,
                          @NotNull TipoProducto tipoProducto, @NotNull @DecimalMin("0.01") BigDecimal precioObjetivo,
                          @NotBlank String moneda) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Seguir un destino: avisa por mail cuando aparezca un precio <= objetivo")
    public SeguimientoDestino crear(@Valid @RequestBody Request r) {
        if (!viajeros.existsById(r.viajeroId())) throw new NotFoundException("No existe el viajero " + r.viajeroId());
        return repo.save(new SeguimientoDestino(r.viajeroId(), r.destino().toUpperCase(), r.tipoProducto().name(),
                r.precioObjetivo(), r.moneda().toUpperCase()));
    }

    @GetMapping
    public List<SeguimientoDestino> listar(@RequestParam Long viajeroId) {
        return repo.findByViajeroId(viajeroId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        SeguimientoDestino s = repo.findById(id).orElseThrow(() -> new NotFoundException("No existe el seguimiento " + id));
        s.desactivar();
        repo.save(s);
    }
}
