package com.travelhub.reservas;

import com.travelhub.common.events.Eventos;
import com.travelhub.reservas.outbox.OutboxEvento;
import com.travelhub.reservas.outbox.OutboxRelay;
import com.travelhub.reservas.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** El relay publica los pendientes y, si RabbitMQ esta caido, los deja pendientes para reintentar. */
class OutboxRelayTest {

    private final OutboxRepository repo = mock(OutboxRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final OutboxRelay relay = new OutboxRelay(repo, rabbit);

    private OutboxEvento evento() {
        return new OutboxEvento(Eventos.RESERVA_CREADA, "com.travelhub.common.events.ReservaCreadaEvent",
                "{\"reservaId\":1}", "cid-1");
    }

    @Test
    void publicaYMarcaComoPublicado() {
        OutboxEvento e = evento();
        when(repo.findTop50ByPublicadoEnIsNullOrderByIdAsc()).thenReturn(List.of(e));

        relay.publicarPendientes();

        verify(rabbit).send(eq(Eventos.EXCHANGE), eq(Eventos.RESERVA_CREADA), any(Message.class));
        assertNotNull(e.getPublicadoEn());
    }

    @Test
    void siRabbitEstaCaidoQuedaPendiente() {
        OutboxEvento e = evento();
        when(repo.findTop50ByPublicadoEnIsNullOrderByIdAsc()).thenReturn(List.of(e));
        doThrow(new AmqpConnectException(new ConnectException("caido")))
                .when(rabbit).send(anyString(), anyString(), any(Message.class));

        relay.publicarPendientes();

        assertNull(e.getPublicadoEn());
        assertEquals(1, e.getIntentos());
    }
}
