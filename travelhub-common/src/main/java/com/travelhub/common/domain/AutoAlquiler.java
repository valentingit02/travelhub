package com.travelhub.common.domain;

public class AutoAlquiler extends ProductoTuristico {

    public AutoAlquiler(String id, String destino, Money precioBase) {
        super(id, destino, precioBase);
    }

    @Override
    public TipoProducto getTipo() { return TipoProducto.AUTO; }

    @Override
    public String descripcion() { return "Auto de alquiler en " + getDestino(); }
}
