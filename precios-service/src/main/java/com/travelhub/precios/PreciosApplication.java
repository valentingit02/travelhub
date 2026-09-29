package com.travelhub.precios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.travelhub")
@ConfigurationPropertiesScan
public class PreciosApplication {
    public static void main(String[] args) {
        SpringApplication.run(PreciosApplication.class, args);
    }
}
