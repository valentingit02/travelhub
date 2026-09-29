package com.travelhub.pagos;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.events.PagoAprobadoEvent;
import com.travelhub.common.events.PagoRechazadoEvent;
import com.travelhub.common.events.ReservaCreadaEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integracion con RabbitMQ y PostgreSQL reales (Testcontainers).
 * Si no hay Docker disponible, se saltea sola.
 */
@SpringBootTest(properties = "travelhub.pagos.demora-ms=0")
@Testcontainers(disabledWithoutDocker = true)
class PagoIntegracionTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired RabbitTemplate template;
    @Autowired RabbitAdmin admin;
    @Autowired PagoRepository pagos;

    private String colaEspia(String routingKey) {
        Queue q = new AnonymousQueue();
        admin.declareQueue(q);
        admin.declareBinding(BindingBuilder.bind(q).to(new TopicExchange(Eventos.EXCHANGE)).with(routingKey));
        return q.getName();
    }

    @Test
    void reservaCreadaGeneraPagoAprobado() {
        String espia = colaEspia(Eventos.PAGO_APROBADO);
        template.convertAndSend(Eventos.EXCHANGE, Eventos.RESERVA_CREADA,
                new ReservaCreadaEvent(100L, 1L, "ana@test.com", new BigDecimal("500"), "USD"));

        Object msg = template.receiveAndConvert(espia, 15000);
        assertInstanceOf(PagoAprobadoEvent.class, msg);
        assertEquals(100L, ((PagoAprobadoEvent) msg).reservaId());
        assertEquals(Pago.Estado.APROBADO, pagos.findByReservaId(100L).orElseThrow().getEstado());
    }

    @Test
    void montoSobreElLimiteGeneraPagoRechazado() {
        String espia = colaEspia(Eventos.PAGO_RECHAZADO);
        template.convertAndSend(Eventos.EXCHANGE, Eventos.RESERVA_CREADA,
                new ReservaCreadaEvent(200L, 1L, "ana@test.com", new BigDecimal("99999"), "USD"));

        Object msg = template.receiveAndConvert(espia, 15000);
        assertInstanceOf(PagoRechazadoEvent.class, msg);
    }
}
