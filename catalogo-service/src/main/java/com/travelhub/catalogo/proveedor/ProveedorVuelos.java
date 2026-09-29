package com.travelhub.catalogo.proveedor;

import java.time.LocalDate;
import java.util.List;

public interface ProveedorVuelos {
    List<OfertaProveedor> buscar(String origen, String destino, LocalDate fecha, int pasajeros);
    String reservar(String ofertaId, Titular titular, int pasajeros, String reservaRef);
    void cancelar(String refExterna);
}
