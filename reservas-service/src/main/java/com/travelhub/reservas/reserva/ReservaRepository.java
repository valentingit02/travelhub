package com.travelhub.reservas.reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByViajeroIdOrderByCreadaEnDesc(Long viajeroId);
    List<Reserva> findAllByOrderByCreadaEnDesc();
    Optional<Reserva> findByIdempotencyKey(String idempotencyKey);
    List<Reserva> findByEstadoAndCreadaEnBefore(EstadoReserva estado, Instant limite);
}
