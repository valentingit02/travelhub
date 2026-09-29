package com.travelhub.reservas.compartida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
    List<Participante> findByReservaIdOrderByIdAsc(Long reservaId);
    Optional<Participante> findByToken(String token);
}
