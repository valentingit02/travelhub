package com.travelhub.catalogo.auto;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Patron Repository: la capa de negocio no conoce SQL ni JPA. */
public interface AutoRepository extends JpaRepository<Auto, Long> {

    /** Autos publicables: activos y sin revision pendiente. */
    List<Auto> findByDestinoIataIgnoreCaseAndActivoTrueAndEnRevisionFalse(String destinoIata);

    List<Auto> findByActivoTrue();
}
