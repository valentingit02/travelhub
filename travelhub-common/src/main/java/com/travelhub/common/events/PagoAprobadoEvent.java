package com.travelhub.common.events;

public record PagoAprobadoEvent(Long reservaId, Long participanteId, Long pagoId, String referencia) { }
