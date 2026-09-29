package com.travelhub.reservas.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvento, Long> {
    List<OutboxEvento> findTop50ByPublicadoEnIsNullOrderByIdAsc();
    long countByPublicadoEnIsNull();
}
