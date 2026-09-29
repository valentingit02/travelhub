package com.travelhub.reservas.mensajeria;

import com.travelhub.common.events.Eventos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservasEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservasEventPublisher.class);
    private final RabbitTemplate rabbit;

    public ReservasEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publicar(String routingKey, Object evento) {
        rabbit.convertAndSend(Eventos.EXCHANGE, routingKey, evento);
        log.info("Publicado {} -> {}", routingKey, evento);
    }
}
