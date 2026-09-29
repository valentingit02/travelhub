package com.travelhub.catalogo.historial;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialPrecioRepository extends JpaRepository<HistorialPrecio, Long> {
    List<HistorialPrecio> findTop100ByOrderByFechaRegistroDesc();
}
