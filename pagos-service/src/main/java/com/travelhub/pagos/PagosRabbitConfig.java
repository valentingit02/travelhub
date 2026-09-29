package com.travelhub.pagos;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.messaging.MensajeriaConfig;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PagosRabbitConfig {

    public static final String COLA_RESERVA_CREADA = "pagos.reserva-creada";

    @Bean
    Queue colaReservaCreada() { return MensajeriaConfig.colaConDlq(COLA_RESERVA_CREADA); }

    @Bean
    Binding bindReservaCreada(Queue colaReservaCreada, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaReservaCreada).to(eventosExchange).with(Eventos.RESERVA_CREADA);
    }
}
