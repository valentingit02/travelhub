package com.travelhub.common.events;

import java.math.BigDecimal;

public record ReembolsoSolicitadoEvent(Long reservaId, Long participanteId, String email, BigDecimal monto,
                                       String moneda, String motivo) { }
