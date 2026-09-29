package com.travelhub.precios.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "CotizarProductoRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class CotizarProductoRequest {
    public SoapItem item;
}
