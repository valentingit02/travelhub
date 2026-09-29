package com.travelhub.catalogo.mensajeria;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.events.PrecioCambiadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Productor de eventos del catalogo. */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate rabbit;

    public EventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void precioCambiado(PrecioCambiadoEvent evento) {
        try {
            rabbit.convertAndSend(Eventos.EXCHANGE, Eventos.PRECIO_CAMBIADO, evento);
            log.info("Publicado {} producto={} precio={}", Eventos.PRECIO_CAMBIADO, evento.productoRef(),
                    evento.precioNuevo());
        } catch (AmqpException e) {
            // El ABM no se cae si RabbitMQ no esta: se registra y se sigue.
            log.error("No se pudo publicar PrecioCambiado para {}: {}", evento.productoRef(), e.getMessage());
        }
    }
}
