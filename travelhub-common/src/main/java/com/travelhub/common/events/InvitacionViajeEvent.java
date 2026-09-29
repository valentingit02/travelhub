package com.travelhub.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** El organizador de un viaje compartido invita a un amigo a pagar su parte. */
public record InvitacionViajeEvent(Long reservaId, String token, String email, String organizador, String destino,
                                   LocalDate desde, LocalDate hasta, BigDecimal monto, String moneda,
                                   Instant venceEn) { }
