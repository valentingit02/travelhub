package com.travelhub.reservas.reserva;

import com.travelhub.reservas.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Servicio de Pedidos del enunciado (aca: Reservas). Requiere sesion: cada viajero opera solo sus reservas. */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas")
@SecurityRequirement(name = "bearer")
public class ReservaController {

    private final ReservaFacade facade;

    public ReservaController(ReservaFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserva un paquete: verifica precios en el catalogo, cotiza, reserva en proveedores (Saga) y solicita el pago")
    public ReservaDtos.Response reservar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReservaDtos.Request request,
            @Parameter(description = "Clave unica por intento de compra: si se repite, devuelve la reserva ya creada")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Long yo = UsuarioActual.exigirMismo(jwt, request.viajeroId());
        return facade.reservarPaquete(request.conViajero(yo), idempotencyKey);
    }

    @GetMapping
    @Operation(summary = "Lista las reservas del viajero de la sesion (viajeroId, si viene, tiene que ser el propio)")
    public List<ReservaDtos.Response> listar(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) Long viajeroId) {
        return facade.listar(UsuarioActual.exigirMismo(jwt, viajeroId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una reserva con su bitacora de Saga")
    public ReservaDtos.Response obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        facade.exigirDuenio(id, UsuarioActual.id(jwt));
        return facade.obtener(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela la reserva y libera todo en los proveedores")
    public ReservaDtos.Response cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        facade.exigirDuenio(id, UsuarioActual.id(jwt));
        return facade.cancelar(id);
    }
}
