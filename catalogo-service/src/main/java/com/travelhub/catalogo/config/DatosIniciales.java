package com.travelhub.catalogo.config;

import com.travelhub.catalogo.auto.Auto;
import com.travelhub.catalogo.auto.AutoRepository;
import com.travelhub.catalogo.auto.CategoriaAuto;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

/** Carga autos de ejemplo la primera vez (no corre en el perfil test). */
@Configuration
@Profile("!test")
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarAutos(AutoRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            repo.saveAll(List.of(
                new Auto("BRC", CategoriaAuto.ECONOMICO, "Fiat", "Cronos", 5, false, new BigDecimal("45.00"), "USD"),
                new Auto("BRC", CategoriaAuto.SUV, "Toyota", "SW4", 7, true, new BigDecimal("120.00"), "USD"),
                new Auto("MDZ", CategoriaAuto.COMPACTO, "Volkswagen", "Polo", 5, false, new BigDecimal("50.00"), "USD"),
                new Auto("MDZ", CategoriaAuto.PICKUP, "Toyota", "Hilux", 5, false, new BigDecimal("110.00"), "USD"),
                new Auto("IGR", CategoriaAuto.ECONOMICO, "Chevrolet", "Onix", 5, false, new BigDecimal("42.00"), "USD"),
                new Auto("USH", CategoriaAuto.SUV, "Renault", "Duster", 5, false, new BigDecimal("95.00"), "USD"),
                new Auto("MAD", CategoriaAuto.PREMIUM, "BMW", "Serie 3", 5, true, new BigDecimal("150.00"), "USD")
            ));
        };
    }
}
