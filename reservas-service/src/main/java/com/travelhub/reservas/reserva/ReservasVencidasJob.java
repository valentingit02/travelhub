package com.travelhub.reservas.reserva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Red de seguridad: toda reserva PENDIENTE cuyo plazo (vence_en) paso se compensa y pasa a FALLIDA.
 * Individual: 15 min sin pago. Compartida: 24 h sin que paguen todos (configurable).
 */
@Component
public class ReservasVencidasJob {

    private static final Logger log = LoggerFactory.getLogger(ReservasVencidasJob.class);

    private final ReservaRepository reservas;
    private final ReservaFacade facade;

    public ReservasVencidasJob(ReservaRepository reservas, ReservaFacade facade) {
        this.reservas = reservas;
        this.facade = facade;
    }

    @Scheduled(fixedDelayString = "${travelhub.reservas.revision-ms:60000}", initialDelay = 30000)
    public void revisar() {
        for (Reserva r : reservas.findByEstadoAndVenceEnBefore(EstadoReserva.PENDIENTE, Instant.now())) {
            try {
                facade.expirar(r.getId());
            } catch (RuntimeException e) {
                log.error("No se pudo expirar la reserva {}: {}", r.getId(), e.getMessage());
            }
        }
    }
}
