package com.travelhub.common.events;

public record PagoRechazadoEvent(Long reservaId, Long pagoId, String motivo) { }
