package com.travelhub.reservas.compartida;

import com.travelhub.reservas.reserva.ReservaFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas/compartidas")
@Tag(name = "Viaje compartido", description = "Cada amigo paga su parte con un link, sin crear cuenta")
public class CompartidaController {

    private final ReservaFacade facade;

    public CompartidaController(ReservaFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Resumen del viaje y la parte a pagar de este participante")
    public CompartidaDtos.Vista ver(@PathVariable String token) {
        return facade.vista(token);
    }

    @PostMapping("/{token}/pagar")
    @Operation(summary = "Paga (o reintenta pagar) la parte de este participante")
    public CompartidaDtos.Vista pagar(@PathVariable String token,
                                      @Valid @RequestBody(required = false) CompartidaDtos.PagarRequest body) {
        return facade.pagarParte(token, body == null ? null : body.nombre());
    }
}
