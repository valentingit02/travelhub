package com.travelhub.catalogo.proveedor;

import java.time.LocalDate;
import java.util.List;

public interface ProveedorHoteles {
    List<OfertaProveedor> buscar(String destino, LocalDate checkIn, LocalDate checkOut, int pasajeros);
    String reservar(String ofertaId, Titular titular, int pasajeros, String reservaRef);
    void cancelar(String refExterna);
}
