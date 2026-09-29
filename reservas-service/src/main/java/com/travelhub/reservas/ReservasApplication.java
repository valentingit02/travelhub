package com.travelhub.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.travelhub")
public class ReservasApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservasApplication.class, args);
    }
}
