package com.travelhub.common.events;

import java.math.BigDecimal;

/** Proteccion de precio: bajo el precio de algo ya reservado y se devuelve la diferencia como credito. */
public record CreditoOtorgadoEvent(Long viajeroId, String email, String nombre, Long reservaId, String producto,
                                   BigDecimal monto, BigDecimal saldo, String moneda) { }
