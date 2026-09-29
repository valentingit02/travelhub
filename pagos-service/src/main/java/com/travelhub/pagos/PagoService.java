package com.travelhub.pagos;

import com.travelhub.common.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Consumidor de PagoSolicitado (una parte de la reserva) y ReembolsoSolicitado.
 * Productor de PagoAprobado / PagoRechazado. Idempotente: una parte ya aprobada no se cobra dos veces.
 */
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

    @RabbitListener(queues = PagosRabbitConfig.COLA_PAGO_SOLICITADO)
    public void onPagoSolicitado(PagoSolicitadoEvent e) {
        if (repo.findFirstByReservaIdAndParticipanteIdAndEstado(e.reservaId(), e.participanteId(), Pago.Estado.APROBADO).isPresent()) {
            log.warn("Reserva {} parte {} ya estaba pagada: mensaje duplicado ignorado", e.reservaId(), e.participanteId());
            return;
        }
        log.info("Cobrando reserva {} parte {}: {} {}", e.reservaId(), e.participanteId(), e.monto(), e.moneda());
        ProcesadorPago.Resultado r = procesador.cobrar(e.reservaId(), e.email(), e.monto(), e.moneda());

        Pago pago = repo.save(new Pago(e.reservaId(), e.participanteId(), e.monto(), e.moneda(), procesador.nombre(),
                r.referencia(), r.aprobado() ? Pago.Estado.APROBADO : Pago.Estado.RECHAZADO, r.motivo()));

        if (r.aprobado()) {
            rabbit.convertAndSend(Eventos.EXCHANGE, Eventos.PAGO_APROBADO,
                    new PagoAprobadoEvent(e.reservaId(), e.participanteId(), pago.getId(), r.referencia()));
            log.info("Pago aprobado reserva={} parte={} ref={}", e.reservaId(), e.participanteId(), r.referencia());
        } else {
            rabbit.convertAndSend(Eventos.EXCHANGE, Eventos.PAGO_RECHAZADO,
                    new PagoRechazadoEvent(e.reservaId(), e.participanteId(), pago.getId(), r.motivo()));
            log.warn("Pago rechazado reserva={} parte={} motivo={}", e.reservaId(), e.participanteId(), r.motivo());
        }
    }

    @RabbitListener(queues = PagosRabbitConfig.COLA_REEMBOLSO)
    public void onReembolso(ReembolsoSolicitadoEvent e) {
        repo.findFirstByReservaIdAndParticipanteIdAndEstado(e.reservaId(), e.participanteId(), Pago.Estado.APROBADO)
                .ifPresentOrElse(p -> {
                    p.reembolsar(e.motivo());
                    repo.save(p);
                    log.info("Reembolsado pago {} ({} {}) de la reserva {}", p.getId(), p.getMonto(), p.getMoneda(), e.reservaId());
                }, () -> log.warn("Reembolso pedido para reserva {} parte {} sin pago aprobado", e.reservaId(), e.participanteId()));
    }
}
