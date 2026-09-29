package com.travelhub.precios.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "CotizarPaqueteResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"cotizacion", "subtotal", "factorPaquete", "motivoDescuento", "total", "moneda"})
public class CotizarPaqueteResponse {
    public List<SoapCotizacion> cotizacion = new ArrayList<>();
    public BigDecimal subtotal;
    public BigDecimal factorPaquete;
    public String motivoDescuento;
    public BigDecimal total;
    public String moneda;
}
