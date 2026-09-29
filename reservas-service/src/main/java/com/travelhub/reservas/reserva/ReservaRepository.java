package com.travelhub.reservas.reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByViajeroIdOrderByCreadaEnDesc(Long viajeroId);
    List<Reserva> findAllByOrderByCreadaEnDesc();
}
