package com.travelhub.common.domain;

public class Excursion extends ProductoTuristico {

    public Excursion(String id, String destino, Money precioBase) {
        super(id, destino, precioBase);
    }

    @Override
    public TipoProducto getTipo() { return TipoProducto.EXCURSION; }

    @Override
    public String descripcion() { return "Excursion en " + getDestino(); }
}
