package com.travelhub.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.travelhub")
@EnableScheduling   // relay del Outbox y vencimiento de reservas
public class ReservasApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservasApplication.class, args);
    }
}
