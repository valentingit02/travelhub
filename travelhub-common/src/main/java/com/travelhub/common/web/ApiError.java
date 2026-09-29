package com.travelhub.common.web;

import java.time.Instant;
import java.util.List;

/** Formato unico de error para todos los servicios REST. */
public record ApiError(int status, String error, String mensaje, String correlationId,
                       Instant timestamp, List<String> detalles) { }
