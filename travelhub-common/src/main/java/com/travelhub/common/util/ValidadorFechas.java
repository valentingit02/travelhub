package com.travelhub.common.util;

import java.time.LocalDate;

/** Componente de utilidad: validaciones de fechas reutilizadas por todos los servicios. */
public final class ValidadorFechas {

    private ValidadorFechas() { }

    public static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (desde.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede estar en el pasado");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");
        }
    }
}
