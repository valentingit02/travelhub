package com.travelhub.common.web;

/** 409: por ejemplo, un auto que ya esta reservado en esas fechas. */
public class ConflictException extends RuntimeException {
    public ConflictException(String mensaje) { super(mensaje); }
}
