package com.travelhub.catalogo.reservaproveedor;

import com.travelhub.catalogo.auto.AutoService;
import com.travelhub.catalogo.proveedor.ProveedorExcursiones;
import com.travelhub.catalogo.proveedor.ProveedorHoteles;
import com.travelhub.catalogo.proveedor.ProveedorVuelos;
import com.travelhub.common.domain.TipoProducto;
import org.springframework.stereotype.Service;

/**
 * Reserva y cancela en el proveedor que corresponda. Lo usa la Saga de reservas-service:
 * asi las APIs externas se consumen solo desde el catalogo.
 */
@Service
public class ReservaProveedorService {

    private final ProveedorVuelos vuelos;
    private final ProveedorHoteles hoteles;
    private final ProveedorExcursiones excursiones;
    private final AutoService autos;

    public ReservaProveedorService(ProveedorVuelos vuelos, ProveedorHoteles hoteles,
                                   ProveedorExcursiones excursiones, AutoService autos) {
        this.vuelos = vuelos;
        this.hoteles = hoteles;
        this.excursiones = excursiones;
        this.autos = autos;
    }

    public ReservaProveedorResponse reservar(ReservaProveedorRequest r) {
        String ref = switch (r.tipo()) {
            case VUELO -> vuelos.reservar(r.productoId(), r.titular(), r.pasajeros(), r.reservaRef());
            case HOTEL -> hoteles.reservar(r.productoId(), r.titular(), r.pasajeros(), r.reservaRef());
            case EXCURSION -> excursiones.reservar(r.productoId(), r.titular(), r.pasajeros(), r.reservaRef());
            case AUTO -> "BLQ-" + autos.bloquear(autoId(r.productoId()), r.desde(), r.hasta(), r.reservaRef()).getId();
        };
        return new ReservaProveedorResponse(r.tipo(), r.productoId(), ref);
    }

    public void cancelar(TipoProducto tipo, String refExterna, String reservaRef) {
        switch (tipo) {
            case VUELO -> vuelos.cancelar(refExterna);
            case HOTEL -> hoteles.cancelar(refExterna);
            case EXCURSION -> excursiones.cancelar(refExterna);
            case AUTO -> autos.liberarBloqueos(reservaRef);
        }
    }

    private Long autoId(String productoId) {
        if (!productoId.startsWith("AUTO-")) {
            throw new IllegalArgumentException("Id de auto invalido: " + productoId);
        }
        return Long.valueOf(productoId.substring(5));
    }
}
