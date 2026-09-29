package com.travelhub.common.events;

import java.math.BigDecimal;

/** Pedido de cobro de UNA parte de la reserva (en un viaje individual hay una sola parte). */
public record PagoSolicitadoEvent(Long reservaId, Long participanteId, String email, BigDecimal monto, String moneda) { }
