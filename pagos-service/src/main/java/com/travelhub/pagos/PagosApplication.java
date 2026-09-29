package com.travelhub.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.travelhub")
public class PagosApplication {
    public static void main(String[] args) {
        SpringApplication.run(PagosApplication.class, args);
    }
}
