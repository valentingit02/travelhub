package com.travelhub.reservas.viajero;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ViajeroRepository extends JpaRepository<Viajero, Long> {
    Optional<Viajero> findByEmailIgnoreCase(String email);
}
