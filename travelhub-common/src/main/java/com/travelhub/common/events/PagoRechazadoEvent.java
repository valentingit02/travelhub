package com.travelhub.common.events;

public record PagoRechazadoEvent(Long reservaId, Long participanteId, Long pagoId, String motivo) { }
