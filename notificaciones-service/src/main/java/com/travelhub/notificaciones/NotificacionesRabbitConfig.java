package com.travelhub.notificaciones;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.messaging.MensajeriaConfig;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NotificacionesRabbitConfig {

    public static final String COLA_CONFIRMADA = "notificaciones.reserva-confirmada";
    public static final String COLA_CANCELADA = "notificaciones.reserva-cancelada";
    public static final String COLA_ALERTA = "notificaciones.alerta-precio";
    public static final String COLA_INVITACION = "notificaciones.invitacion-viaje";
    public static final String COLA_CREDITO = "notificaciones.credito-otorgado";

    @Bean Queue colaConfirmada() { return MensajeriaConfig.colaConDlq(COLA_CONFIRMADA); }
    @Bean Queue colaCancelada() { return MensajeriaConfig.colaConDlq(COLA_CANCELADA); }
    @Bean Queue colaAlerta() { return MensajeriaConfig.colaConDlq(COLA_ALERTA); }
    @Bean Queue colaInvitacion() { return MensajeriaConfig.colaConDlq(COLA_INVITACION); }
    @Bean Queue colaCredito() { return MensajeriaConfig.colaConDlq(COLA_CREDITO); }

    @Bean
    Binding bindConfirmada(Queue colaConfirmada, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaConfirmada).to(eventosExchange).with(Eventos.RESERVA_CONFIRMADA);
    }

    @Bean
    Binding bindCancelada(Queue colaCancelada, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaCancelada).to(eventosExchange).with(Eventos.RESERVA_CANCELADA);
    }

    @Bean
    Binding bindAlerta(Queue colaAlerta, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaAlerta).to(eventosExchange).with(Eventos.ALERTA_PRECIO);
    }

    @Bean
    Binding bindInvitacion(Queue colaInvitacion, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaInvitacion).to(eventosExchange).with(Eventos.INVITACION_VIAJE);
    }

    @Bean
    Binding bindCredito(Queue colaCredito, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaCredito).to(eventosExchange).with(Eventos.CREDITO_OTORGADO);
    }
}
