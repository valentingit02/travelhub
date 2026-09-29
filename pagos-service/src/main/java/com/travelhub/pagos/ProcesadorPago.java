package com.travelhub.pagos;

import java.math.BigDecimal;

/**
 * Strategy de medio de pago. Hoy hay una implementacion simulada; para sumar
 * Mercado Pago u otra pasarela se agrega otra clase sin tocar el consumidor.
 */
public interface ProcesadorPago {

    record Resultado(boolean aprobado, String referencia, String motivo) { }

    String nombre();

    Resultado cobrar(Long reservaId, String email, BigDecimal monto, String moneda);
}
