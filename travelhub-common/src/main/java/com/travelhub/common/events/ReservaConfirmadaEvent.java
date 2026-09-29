package com.travelhub.common.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReservaConfirmadaEvent(Long reservaId, String email, String nombre, String destino,
                                     LocalDate desde, LocalDate hasta, int pasajeros,
                                     List<Item> items, BigDecimal total, String moneda) {

    public record Item(String tipo, String descripcion, BigDecimal precio) { }
}
