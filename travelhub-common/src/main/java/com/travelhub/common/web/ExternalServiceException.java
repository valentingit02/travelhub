package com.travelhub.common.web;

/** 502: fallo un proveedor externo o un servicio interno remoto. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String mensaje) { super(mensaje); }
    public ExternalServiceException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
