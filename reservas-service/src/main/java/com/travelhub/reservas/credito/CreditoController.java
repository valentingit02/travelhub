package com.travelhub.reservas.credito;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/viajeros/{viajeroId}/creditos")
@Tag(name = "Creditos", description = "Proteccion de precio: saldo a favor del viajero")
public class CreditoController {

    private final CreditoService service;

    public CreditoController(CreditoService service) {
        this.service = service;
    }

    public record Saldo(BigDecimal saldo, String moneda, List<MovimientoCredito> movimientos) { }

    @GetMapping
    @Operation(summary = "Saldo de credito y movimientos (se aplica solo en la proxima reserva individual)")
    public Saldo saldo(@PathVariable Long viajeroId) {
        return new Saldo(service.saldo(viajeroId), "USD", service.movimientos(viajeroId));
    }
}
