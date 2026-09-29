package com.travelhub.common.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** acompanantes: emails de los amigos de un viaje compartido (vacia si es individual). */
public record ReservaConfirmadaEvent(Long reservaId, String email, String nombre, String destino,
                                     LocalDate desde, LocalDate hasta, int pasajeros,
                                     List<Item> items, BigDecimal total, String moneda,
                                     List<String> acompanantes) {

    public record Item(String tipo, String descripcion, BigDecimal precio) { }
}
