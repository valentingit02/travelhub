package com.travelhub.common.events;

import java.math.BigDecimal;

public record ReservaCreadaEvent(Long reservaId, Long viajeroId, String email, BigDecimal total, String moneda) { }
