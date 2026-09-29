package com.travelhub.reservas.credito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCreditoRepository extends JpaRepository<MovimientoCredito, Long> {
    List<MovimientoCredito> findByViajeroIdOrderByIdDesc(Long viajeroId);
}
