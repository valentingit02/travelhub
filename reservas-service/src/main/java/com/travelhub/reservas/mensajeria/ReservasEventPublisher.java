package com.travelhub.reservas.mensajeria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.common.web.CorrelationIdFilter;
import com.travelhub.reservas.outbox.OutboxEvento;
import com.travelhub.reservas.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ya no publica directo en RabbitMQ: guarda el evento en la tabla outbox dentro de la
 * transaccion actual. Si la transaccion hace rollback, el evento tampoco existe.
 */
@Component
public class ReservasEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservasEventPublisher.class);

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public ReservasEventPublisher(OutboxRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional
    public void publicar(String routingKey, Object evento) {
        try {
            String json = mapper.writeValueAsString(evento);
            outbox.save(new OutboxEvento(routingKey, evento.getClass().getName(), json,
                    MDC.get(CorrelationIdFilter.MDC_KEY)));
            log.info("Evento {} guardado en outbox", routingKey);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + routingKey, e);
        }
    }
}
