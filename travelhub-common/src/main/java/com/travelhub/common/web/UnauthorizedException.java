package com.travelhub.common.web;

/** 401: credenciales incorrectas o sesion invalida. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String mensaje) { super(mensaje); }
}
