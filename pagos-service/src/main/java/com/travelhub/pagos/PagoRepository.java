package com.travelhub.pagos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByReservaIdOrderByIdAsc(Long reservaId);
    Optional<Pago> findFirstByReservaIdAndParticipanteIdAndEstado(Long reservaId, Long participanteId, Pago.Estado estado);
}
