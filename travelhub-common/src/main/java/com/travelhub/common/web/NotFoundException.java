package com.travelhub.common.web;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String mensaje) { super(mensaje); }
}
