package com.travelhub.catalogo.oferta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfertaPublicadaRepository extends JpaRepository<OfertaPublicada, String> {
    List<OfertaPublicada> findByBloqueadaTrue();
}
