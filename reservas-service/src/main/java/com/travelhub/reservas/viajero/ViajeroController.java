package com.travelhub.reservas.viajero;

import com.travelhub.common.web.ConflictException;
import com.travelhub.common.web.NotFoundException;
import com.travelhub.common.web.UnauthorizedException;
import com.travelhub.reservas.auth.AuthService;
import com.travelhub.reservas.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Servicio de Clientes del enunciado (aca: Viajeros). Cada viajero solo ve y edita su propio perfil. */
@RestController
@RequestMapping("/api/viajeros")
@Tag(name = "Viajeros")
public class ViajeroController {

    private final ViajeroRepository repo;
    private final AuthService auth;

    public ViajeroController(ViajeroRepository repo, AuthService auth) {
        this.repo = repo;
        this.auth = auth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un viajero sin contrasena (compatibilidad). Preferir POST /api/auth/registro",
            deprecated = true)
    public ViajeroDtos.Response crear(@Valid @RequestBody ViajeroDtos.Request r) {
        repo.findByEmailIgnoreCase(r.email()).ifPresent(v -> {
            throw new ConflictException("Ya existe un viajero con el email " + r.email());
        });
        Viajero v = repo.save(new Viajero(r.nombre(), r.apellido(), r.email().toLowerCase(), r.documento(),
                r.fechaNacimiento(), r.preferencias()));
        return ViajeroDtos.Response.from(v);
    }

    @GetMapping
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Devuelve solo al viajero de la sesion (ya no lista a todos: privacidad)")
    public List<ViajeroDtos.Response> listar(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) String email) {
        Viajero yo = actual(jwt);
        if (email != null && !email.isBlank() && !yo.getEmail().equalsIgnoreCase(email.trim())) {
            return List.of();
        }
        return List.of(ViajeroDtos.Response.from(yo));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Perfil del viajero de la sesion")
    public ViajeroDtos.Response yo(@AuthenticationPrincipal Jwt jwt) {
        return ViajeroDtos.Response.from(actual(jwt));
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Actualiza el perfil (el email no se cambia: es el usuario de ingreso)")
    public ViajeroDtos.Response actualizar(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody ViajeroDtos.PerfilRequest r) {
        Viajero v = actual(jwt);
        String documento = ViajeroDtos.limpio(r.documento(), false);
        v.actualizarPerfil(r.nombre().trim(), r.apellido().trim(), documento == null ? v.getDocumento() : documento,
                r.fechaNacimiento(),
                ViajeroDtos.limpio(r.preferencias(), false), ViajeroDtos.limpio(r.telefono(), false),
                ViajeroDtos.limpio(r.aeropuertoOrigen(), true));
        return ViajeroDtos.Response.from(repo.save(v));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Cambia la contrasena (pide la actual)")
    public void cambiarPassword(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody ViajeroDtos.CambioPasswordRequest r) {
        auth.cambiarPassword(UsuarioActual.id(jwt), r.actual(), r.nueva());
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearer")
    public ViajeroDtos.Response obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        UsuarioActual.exigirMismo(jwt, id);
        return repo.findById(id).map(ViajeroDtos.Response::from)
                .orElseThrow(() -> new NotFoundException("No existe el viajero " + id));
    }

    private Viajero actual(Jwt jwt) {
        return repo.findById(UsuarioActual.id(jwt))
                .orElseThrow(() -> new UnauthorizedException("Tu cuenta ya no existe. Ingresa de nuevo."));
    }
}
