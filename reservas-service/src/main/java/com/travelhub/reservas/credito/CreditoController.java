package com.travelhub.reservas.credito;

import com.travelhub.reservas.seguridad.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/viajeros/{viajeroId}/creditos")
@Tag(name = "Creditos", description = "Proteccion de precio: saldo a favor del viajero")
@SecurityRequirement(name = "bearer")
public class CreditoController {

    private final CreditoService service;

    public CreditoController(CreditoService service) {
        this.service = service;
    }

    public record Saldo(BigDecimal saldo, String moneda, List<MovimientoCredito> movimientos) { }

    @GetMapping
    @Operation(summary = "Saldo de credito y movimientos (se aplica solo en la proxima reserva individual)")
    public Saldo saldo(@AuthenticationPrincipal Jwt jwt, @PathVariable Long viajeroId) {
        UsuarioActual.exigirMismo(jwt, viajeroId);
        return new Saldo(service.saldo(viajeroId), "USD", service.movimientos(viajeroId));
    }
}
