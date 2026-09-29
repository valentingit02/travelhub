package com.travelhub.catalogo.mensajeria;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.messaging.MensajeriaConfig;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CatalogoRabbitConfig {

    public static final String COLA_ANOMALIAS = "catalogo.anomalia-detectada";
    public static final String COLA_CANCELACIONES = "catalogo.reserva-cancelada";

    @Bean
    Queue colaAnomalias() { return MensajeriaConfig.colaConDlq(COLA_ANOMALIAS); }

    @Bean
    Queue colaCancelaciones() { return MensajeriaConfig.colaConDlq(COLA_CANCELACIONES); }

    @Bean
    Binding bindAnomalias(Queue colaAnomalias, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaAnomalias).to(eventosExchange).with(Eventos.ANOMALIA_DETECTADA);
    }

    @Bean
    Binding bindCancelaciones(Queue colaCancelaciones, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaCancelaciones).to(eventosExchange).with(Eventos.RESERVA_CANCELADA);
    }
}
