package com.travelhub.reservas.outbox;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Relay del Outbox: cada segundo publica en RabbitMQ los eventos pendientes, en orden.
 * Si RabbitMQ esta caido, corta y reintenta en la siguiente vuelta (nada se pierde).
 * Entrega "al menos una vez": los consumidores son idempotentes.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repo;
    private final RabbitTemplate rabbit;

    public OutboxRelay(OutboxRepository repo, RabbitTemplate rabbit) {
        this.repo = repo;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelayString = "${travelhub.outbox.intervalo-ms:1000}")
    public void publicarPendientes() {
        for (OutboxEvento e : repo.findTop50ByPublicadoEnIsNullOrderByIdAsc()) {
            try {
                rabbit.send(Eventos.EXCHANGE, e.getRoutingKey(), aMensaje(e));
                e.marcarPublicado();
                repo.save(e);
                log.info("Outbox: publicado {} (evento #{})", e.getRoutingKey(), e.getId());
            } catch (AmqpException ex) {
                e.registrarFallo();
                repo.save(e);
                log.warn("Outbox: RabbitMQ no disponible, {} queda pendiente (intento {}): {}",
                        e.getRoutingKey(), e.getIntentos(), ex.getMessage());
                return; // se respeta el orden: se reintenta en la proxima vuelta
            }
        }
    }

    static Message aMensaje(OutboxEvento e) {
        MessageProperties p = new MessageProperties();
        p.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        p.setContentEncoding(StandardCharsets.UTF_8.name());
        p.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        p.setMessageId("outbox-" + e.getId());
        p.setHeader("__TypeId__", e.getTipo());
        if (e.getCorrelationId() != null) {
            p.setHeader(CorrelationIdFilter.HEADER, e.getCorrelationId());
        }
        return new Message(e.getPayload().getBytes(StandardCharsets.UTF_8), p);
    }
}
