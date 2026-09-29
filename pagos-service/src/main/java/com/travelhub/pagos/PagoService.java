package com.travelhub.pagos;

import com.travelhub.common.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Consumidor de ReservaCreada y productor de PagoAprobado / PagoRechazado. */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository repo;
    private final ProcesadorPago procesador;
    private final RabbitTemplate rabbit;

    public PagoService(PagoRepository repo, ProcesadorPago procesador, RabbitTemplate rabbit) {
        this.repo = repo;
        this.procesador = procesador;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = PagosRabbitConfig.COLA_RESERVA_CREADA)
    public void onReservaCreada(ReservaCreadaEvent e) {
        if (repo.findByReservaId(e.reservaId()).isPresent()) {
            log.warn("Reserva {} ya tenia pago: mensaje duplicado ignorado", e.reservaId());
            return; // idempotencia
        }
        log.info("Cobrando reserva {}: {} {}", e.reservaId(), e.total(), e.moneda());
        ProcesadorPago.Resultado r = procesador.cobrar(e.reservaId(), e.email(), e.total(), e.moneda());

        Pago pago = repo.save(new Pago(e.reservaId(), e.total(), e.moneda(), procesador.nombre(), r.referencia(),
                r.aprobado() ? Pago.Estado.APROBADO : Pago.Estado.RECHAZADO, r.motivo()));

        if (r.aprobado()) {
            rabbit.convertAndSend(Eventos.EXCHANGE, Eventos.PAGO_APROBADO,
                    new PagoAprobadoEvent(e.reservaId(), pago.getId(), r.referencia()));
            log.info("Pago aprobado reserva={} ref={}", e.reservaId(), r.referencia());
        } else {
            rabbit.convertAndSend(Eventos.EXCHANGE, Eventos.PAGO_RECHAZADO,
                    new PagoRechazadoEvent(e.reservaId(), pago.getId(), r.motivo()));
            log.warn("Pago rechazado reserva={} motivo={}", e.reservaId(), r.motivo());
        }
    }
}
