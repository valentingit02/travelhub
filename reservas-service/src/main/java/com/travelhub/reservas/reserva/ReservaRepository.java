package com.travelhub.reservas.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByViajeroIdOrderByCreadaEnDesc(Long viajeroId);
    List<Reserva> findAllByOrderByCreadaEnDesc();
    Optional<Reserva> findByIdempotencyKey(String idempotencyKey);
    List<Reserva> findByEstadoAndVenceEnBefore(EstadoReserva estado, Instant limite);

    /** Reservas con viaje futuro que incluyen el producto (para la proteccion de precio). */
    @Query("select distinct r from Reserva r join r.items i "
            + "where i.productoId = :ref and r.estado = :estado and r.desde > :hoy")
    List<Reserva> conProducto(@Param("ref") String productoRef, @Param("estado") EstadoReserva estado,
                              @Param("hoy") LocalDate hoy);
}
