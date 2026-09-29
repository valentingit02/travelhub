package com.travelhub.precios.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Cotizacion", propOrder = {"tipoProducto", "precioBase", "unidades", "unidad", "subtotal", "factor", "precioFinal", "moneda"})
public class SoapCotizacion {
    public String tipoProducto;
    public BigDecimal precioBase;
    public long unidades;
    public String unidad;
    public BigDecimal subtotal;
    public List<SoapFactor> factor = new ArrayList<>();
    public BigDecimal precioFinal;
    public String moneda;
}
