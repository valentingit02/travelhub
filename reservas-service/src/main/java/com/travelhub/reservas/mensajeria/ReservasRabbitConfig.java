package com.travelhub.reservas.mensajeria;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.messaging.MensajeriaConfig;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReservasRabbitConfig {

    public static final String COLA_PAGO_APROBADO = "reservas.pago-aprobado";
    public static final String COLA_PAGO_RECHAZADO = "reservas.pago-rechazado";
    public static final String COLA_PRECIO_CAMBIADO = "reservas.precio-cambiado";

    @Bean Queue colaPagoAprobado() { return MensajeriaConfig.colaConDlq(COLA_PAGO_APROBADO); }
    @Bean Queue colaPagoRechazado() { return MensajeriaConfig.colaConDlq(COLA_PAGO_RECHAZADO); }
    @Bean Queue colaPrecioCambiado() { return MensajeriaConfig.colaConDlq(COLA_PRECIO_CAMBIADO); }

    @Bean
    Binding bindPagoAprobado(Queue colaPagoAprobado, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaPagoAprobado).to(eventosExchange).with(Eventos.PAGO_APROBADO);
    }

    @Bean
    Binding bindPagoRechazado(Queue colaPagoRechazado, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaPagoRechazado).to(eventosExchange).with(Eventos.PAGO_RECHAZADO);
    }

    @Bean
    Binding bindPrecioCambiado(Queue colaPrecioCambiado, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaPrecioCambiado).to(eventosExchange).with(Eventos.PRECIO_CAMBIADO);
    }
}
