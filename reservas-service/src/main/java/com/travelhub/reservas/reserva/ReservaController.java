package com.travelhub.reservas.reserva;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Servicio de Pedidos del enunciado (aca: Reservas). */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas")
public class ReservaController {

    private final ReservaFacade facade;

    public ReservaController(ReservaFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserva un paquete: cotiza, reserva en proveedores (Saga) y solicita el pago")
    public ReservaDtos.Response reservar(@Valid @RequestBody ReservaDtos.Request request) {
        return facade.reservarPaquete(request);
    }

    @GetMapping
    @Operation(summary = "Lista reservas (opcionalmente de un viajero)")
    public List<ReservaDtos.Response> listar(@RequestParam(required = false) Long viajeroId) {
        return facade.listar(viajeroId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una reserva con su bitacora de Saga")
    public ReservaDtos.Response obtener(@PathVariable Long id) {
        return facade.obtener(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela la reserva y libera todo en los proveedores")
    public ReservaDtos.Response cancelar(@PathVariable Long id) {
        return facade.cancelar(id);
    }
}
