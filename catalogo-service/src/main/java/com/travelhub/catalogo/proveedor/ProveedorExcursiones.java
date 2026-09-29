package com.travelhub.catalogo.proveedor;

import java.time.LocalDate;
import java.util.List;

public interface ProveedorExcursiones {
    List<OfertaProveedor> buscar(String destino, LocalDate desde, LocalDate hasta);
    String reservar(String ofertaId, Titular titular, int pasajeros, String reservaRef);
    void cancelar(String refExterna);
}
