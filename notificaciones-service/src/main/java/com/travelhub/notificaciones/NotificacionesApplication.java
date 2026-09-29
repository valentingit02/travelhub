package com.travelhub.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.travelhub")
public class NotificacionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificacionesApplication.class, args);
    }
}
