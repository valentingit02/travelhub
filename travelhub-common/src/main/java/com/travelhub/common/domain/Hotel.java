package com.travelhub.common.domain;

public class Hotel extends ProductoTuristico {

    public Hotel(String id, String destino, Money precioBase) {
        super(id, destino, precioBase);
    }

    @Override
    public TipoProducto getTipo() { return TipoProducto.HOTEL; }

    @Override
    public String descripcion() { return "Hotel en " + getDestino(); }
}
