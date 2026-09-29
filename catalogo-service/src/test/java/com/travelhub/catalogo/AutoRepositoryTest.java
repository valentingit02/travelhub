package com.travelhub.catalogo;

import com.travelhub.catalogo.auto.Auto;
import com.travelhub.catalogo.auto.AutoRepository;
import com.travelhub.catalogo.auto.CategoriaAuto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Prueba de la capa de datos contra H2 en memoria (Flyway desactivado: el esquema lo crea Hibernate). */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("test")
class AutoRepositoryTest {

    @Autowired
    AutoRepository repo;

    @Test
    void filtraPorDestinoYSoloActivosSinRevision() {
        repo.save(new Auto("BRC", CategoriaAuto.SUV, "Toyota", "SW4", 7, true, new BigDecimal("120"), "USD"));
        Auto inactivo = new Auto("BRC", CategoriaAuto.ECONOMICO, "Fiat", "Cronos", 5, false, new BigDecimal("45"), "USD");
        inactivo.desactivar();
        repo.save(inactivo);
        Auto enRevision = new Auto("BRC", CategoriaAuto.COMPACTO, "VW", "Gol", 5, false, new BigDecimal("1"), "USD");
        enRevision.marcarEnRevision();
        repo.save(enRevision);
        repo.save(new Auto("MDZ", CategoriaAuto.COMPACTO, "VW", "Polo", 5, false, new BigDecimal("50"), "USD"));

        assertEquals(1, repo.findByDestinoIataIgnoreCaseAndActivoTrueAndEnRevisionFalse("brc").size());
    }
}
