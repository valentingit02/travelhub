package com.travelhub.precios.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Factor", propOrder = {"estrategia", "factor", "motivo"})
public class SoapFactor {
    public String estrategia;
    public BigDecimal factor;
    public String motivo;
}
