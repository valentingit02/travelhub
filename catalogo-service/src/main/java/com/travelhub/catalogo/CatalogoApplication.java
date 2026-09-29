package com.travelhub.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// scanBasePackages incluye com.travelhub.common (filtro de correlacion y manejo de errores)
@SpringBootApplication(scanBasePackages = "com.travelhub")
@ConfigurationPropertiesScan
public class CatalogoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogoApplication.class, args);
    }
}
