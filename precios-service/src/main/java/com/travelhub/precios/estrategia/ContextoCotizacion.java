package com.travelhub.precios.estrategia;

import com.travelhub.common.domain.TipoProducto;

import java.time.LocalDate;

/** Datos que necesita cualquier estrategia para calcular su factor. */
public record ContextoCotizacion(TipoProducto tipo, LocalDate fechaInicio, LocalDate fechaFin,
                                 int pasajeros, double ocupacion) { }
