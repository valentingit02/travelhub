package com.travelhub.precios.soap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/** Se traduce a un SOAP Fault de tipo Client (el error es del que llama). */
@SoapFault(faultCode = FaultCode.CLIENT)
public class CotizacionInvalidaException extends RuntimeException {
    public CotizacionInvalidaException(String mensaje) { super(mensaje); }
}
