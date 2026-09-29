package com.travelhub.common.domain;

/** Componente de dominio base: todo lo que se puede vender en TravelHub. */
public abstract class ProductoTuristico {

    private final String id;
    private final String destino;      // codigo IATA, ej. BRC
    private final Money precioBase;

    protected ProductoTuristico(String id, String destino, Money precioBase) {
        this.id = id;
        this.destino = destino;
        this.precioBase = precioBase;
    }

    public abstract TipoProducto getTipo();

    /** Descripcion corta para mostrar en listados y en el resumen de IA. */
    public abstract String descripcion();

    public String getId() { return id; }
    public String getDestino() { return destino; }
    public Money getPrecioBase() { return precioBase; }
}
