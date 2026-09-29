package com.travelhub.reservas.seguimiento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeguimientoRepository extends JpaRepository<SeguimientoDestino, Long> {
    List<SeguimientoDestino> findByDestinoIgnoreCaseAndTipoProductoAndActivoTrue(String destino, String tipoProducto);
    List<SeguimientoDestino> findByViajeroId(Long viajeroId);
}
