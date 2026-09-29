package com.travelhub.pagos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pasarela simulada con reglas predecibles para la demo:
 *  - monto mayor al limite -> rechazado (fondos insuficientes)
 *  - email que contiene "rechazo" -> rechazado (tarjeta denegada)
 *  - demora configurable para que se vea que el flujo es asincronico.
 */
@Component
public class ProcesadorPagoSimulado implements ProcesadorPago {

    private final BigDecimal limite;
    private final long demoraMs;

    public ProcesadorPagoSimulado(@Value("${travelhub.pagos.limite:10000}") BigDecimal limite,
                                  @Value("${travelhub.pagos.demora-ms:2000}") long demoraMs) {
        this.limite = limite;
        this.demoraMs = demoraMs;
    }

    @Override
    public String nombre() { return "SIMULADO"; }

    @Override
    public Resultado cobrar(Long reservaId, String email, BigDecimal monto, String moneda) {
        try {
            Thread.sleep(demoraMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (monto.compareTo(limite) > 0) {
            return new Resultado(false, null, "Fondos insuficientes (limite " + limite + " " + moneda + ")");
        }
        if (email != null && email.toLowerCase().contains("rechazo")) {
            return new Resultado(false, null, "Tarjeta denegada por el emisor");
        }
        return new Resultado(true, "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), null);
    }
}
