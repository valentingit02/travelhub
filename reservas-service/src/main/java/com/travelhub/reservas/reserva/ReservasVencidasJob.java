package com.travelhub.reservas.reserva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Red de seguridad: una reserva que sigue PENDIENTE despues del vencimiento (el pago nunca llego)
 * se compensa y pasa a FALLIDA. Evita reservas "colgadas" con proveedores bloqueados.
 */
@Component
public class ReservasVencidasJob {

    private static final Logger log = LoggerFactory.getLogger(ReservasVencidasJob.class);

    private final ReservaRepository reservas;
    private final ReservaFacade facade;
    private final Duration vencimiento;

    public ReservasVencidasJob(ReservaRepository reservas, ReservaFacade facade,
                               @Value("${travelhub.reservas.vencimiento:15m}") Duration vencimiento) {
        this.reservas = reservas;
        this.facade = facade;
        this.vencimiento = vencimiento;
    }

    @Scheduled(fixedDelayString = "${travelhub.reservas.revision-ms:60000}", initialDelay = 30000)
    public void revisar() {
        Instant limite = Instant.now().minus(vencimiento);
        for (Reserva r : reservas.findByEstadoAndCreadaEnBefore(EstadoReserva.PENDIENTE, limite)) {
            try {
                facade.expirar(r.getId(), vencimiento);
            } catch (RuntimeException e) {
                log.error("No se pudo expirar la reserva {}: {}", r.getId(), e.getMessage());
            }
        }
    }
}
