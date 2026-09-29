package com.travelhub.reservas.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SagaLogRepository extends JpaRepository<SagaLog, Long> {
    List<SagaLog> findByReservaIdOrderByIdAsc(Long reservaId);
}
