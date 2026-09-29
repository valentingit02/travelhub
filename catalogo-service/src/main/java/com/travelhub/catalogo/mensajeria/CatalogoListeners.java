package com.travelhub.catalogo.mensajeria;

import com.travelhub.catalogo.auto.AutoService;
import com.travelhub.common.events.AnomaliaDetectadaEvent;
import com.travelhub.common.events.ReservaCanceladaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumidores de eventos del catalogo (patron Observer via RabbitMQ). */
@Component
public class CatalogoListeners {

    private static final Logger log = LoggerFactory.getLogger(CatalogoListeners.class);
    private final AutoService autoService;

    public CatalogoListeners(AutoService autoService) {
        this.autoService = autoService;
    }

    @RabbitListener(queues = CatalogoRabbitConfig.COLA_ANOMALIAS)
    public void onAnomalia(AnomaliaDetectadaEvent e) {
        log.warn("IA detecto precio anomalo en {} ({} {}): {}", e.productoRef(), e.precio(), e.moneda(), e.motivo());
        if (e.productoRef() != null && e.productoRef().startsWith("AUTO-")) {
            autoService.marcarEnRevision(Long.valueOf(e.productoRef().substring(5)));
        }
    }

    @RabbitListener(queues = CatalogoRabbitConfig.COLA_CANCELACIONES)
    public void onReservaCancelada(ReservaCanceladaEvent e) {
        int liberados = autoService.liberarBloqueos("RES-" + e.reservaId());
        log.info("Reserva {} cancelada: {} bloqueos de autos liberados", e.reservaId(), liberados);
    }
}
