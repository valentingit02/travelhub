package com.travelhub.precios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Reloj inyectable: permite fijar "hoy" en los tests de anticipacion. */
@Configuration
public class ClockConfig {
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }
}
