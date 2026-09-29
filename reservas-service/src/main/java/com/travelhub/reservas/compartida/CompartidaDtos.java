package com.travelhub.reservas.compartida;

import com.travelhub.reservas.reserva.EstadoReserva;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CompartidaDtos {

    private CompartidaDtos() { }

    public record PagarRequest(@Size(max = 120) String nombre) { }

    public record Item(String tipo, String descripcion) { }

    /** Lo que ve un invitado al abrir su link de pago (no expone datos de los demas). */
    public record Vista(String token, Long reservaId, String destino, LocalDate desde, LocalDate hasta,
                        Integer pasajeros, String organizador, String email, String nombre, BigDecimal monto,
                        String moneda, EstadoParticipante estado, String motivo, EstadoReserva estadoReserva,
                        Instant venceEn, long pagaron, int participantes, BigDecimal total, List<Item> items) { }
}
