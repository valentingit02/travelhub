package com.travelhub.reservas.seguimiento;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.ForbiddenException;
import com.travelhub.common.web.NotFoundException;
import com.travelhub.reservas.seguridad.UsuarioActual;
import com.travelhub.reservas.viajero.ViajeroRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/seguimientos")
@Tag(name = "Alertas de precio")
@SecurityRequirement(name = "bearer")
public class SeguimientoController {

    private final SeguimientoRepository repo;
    private final ViajeroRepository viajeros;

    public SeguimientoController(SeguimientoRepository repo, ViajeroRepository viajeros) {
        this.repo = repo;
        this.viajeros = viajeros;
    }

    /** viajeroId es opcional: se toma de la sesion (si viene, tiene que coincidir). */
    public record Request(Long viajeroId, @NotBlank @Size(min = 3, max = 3) String destino,
                          @NotNull TipoProducto tipoProducto, @NotNull @DecimalMin("0.01") BigDecimal precioObjetivo,
                          @NotBlank String moneda) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Seguir un destino: avisa por mail cuando aparezca un precio <= objetivo")
    public SeguimientoDestino crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Request r) {
        Long yo = UsuarioActual.exigirMismo(jwt, r.viajeroId());
        if (!viajeros.existsById(yo)) throw new NotFoundException("No existe el viajero " + yo);
        return repo.save(new SeguimientoDestino(yo, r.destino().toUpperCase(), r.tipoProducto().name(),
                r.precioObjetivo(), r.moneda().toUpperCase()));
    }

    @GetMapping
    public List<SeguimientoDestino> listar(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required = false) Long viajeroId) {
        return repo.findByViajeroId(UsuarioActual.exigirMismo(jwt, viajeroId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        SeguimientoDestino s = repo.findById(id).orElseThrow(() -> new NotFoundException("No existe el seguimiento " + id));
        if (!s.getViajeroId().equals(UsuarioActual.id(jwt))) {
            throw new ForbiddenException("El seguimiento " + id + " es de otro viajero.");
        }
        s.desactivar();
        repo.save(s);
    }
}
