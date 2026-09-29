package com.travelhub.pagos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByReservaId(Long reservaId);
}
