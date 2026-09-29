package com.travelhub.precios.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "CotizarPaqueteRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class CotizarPaqueteRequest {
    public List<SoapItem> item = new ArrayList<>();
}
