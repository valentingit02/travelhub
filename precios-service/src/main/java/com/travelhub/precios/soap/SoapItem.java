package com.travelhub.precios.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Item", propOrder = {"tipoProducto", "precioBase", "moneda", "fechaInicio", "fechaFin", "pasajeros", "ocupacion"})
public class SoapItem {
    public String tipoProducto;
    public BigDecimal precioBase;
    public String moneda;
    public String fechaInicio;   // xs:date, formato AAAA-MM-DD
    public String fechaFin;
    public int pasajeros;
    public double ocupacion;
}
