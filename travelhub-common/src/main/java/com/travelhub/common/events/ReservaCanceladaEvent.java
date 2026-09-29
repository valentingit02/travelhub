package com.travelhub.common.events;

import java.util.List;

public record ReservaCanceladaEvent(Long reservaId, String email, String nombre, String estado, String motivo,
                                    List<String> acompanantes) { }
