package com.travelhub.catalogo.reservaproveedor;

import com.travelhub.common.domain.TipoProducto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo/reservas-proveedor")
@Tag(name = "Reservas en proveedores", description = "Uso interno de la Saga de reservas-service")
public class ReservaProveedorController {

    private final ReservaProveedorService service;

    public ReservaProveedorController(ReservaProveedorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserva un producto en su proveedor (Duffel, Hotelbeds o inventario propio)")
    public ReservaProveedorResponse reservar(@Valid @RequestBody ReservaProveedorRequest request) {
        return service.reservar(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Compensacion: cancela una reserva en el proveedor")
    public void cancelar(@RequestParam TipoProducto tipo, @RequestParam String ref, @RequestParam String reservaRef) {
        service.cancelar(tipo, ref, reservaRef);
    }
}
