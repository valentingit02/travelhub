package com.travelhub.common.events;

public record PagoAprobadoEvent(Long reservaId, Long pagoId, String referencia) { }
