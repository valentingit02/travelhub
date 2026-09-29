package com.travelhub.common.domain;

public class Vuelo extends ProductoTuristico {

    public Vuelo(String id, String destino, Money precioBase) {
        super(id, destino, precioBase);
    }

    @Override
    public TipoProducto getTipo() { return TipoProducto.VUELO; }

    @Override
    public String descripcion() { return "Vuelo a " + getDestino(); }
}
