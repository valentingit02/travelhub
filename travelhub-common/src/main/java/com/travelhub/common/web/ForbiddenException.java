package com.travelhub.common.web;

/** 403: el usuario esta identificado pero el recurso es de otra persona. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String mensaje) { super(mensaje); }
}
