package com.travelhub.common.events;

public record ReservaCanceladaEvent(Long reservaId, String email, String nombre, String estado, String motivo) { }
