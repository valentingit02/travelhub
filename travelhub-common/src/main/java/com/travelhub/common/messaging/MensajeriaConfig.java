package com.travelhub.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelhub.common.events.Eventos;
import com.travelhub.common.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion comun de RabbitMQ para todos los servicios que tengan spring-boot-starter-amqp:
 * exchange de eventos, dead letter exchange/queue, JSON y propagacion del correlationId.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@ConditionalOnProperty(name = "travelhub.mensajeria.habilitada", havingValue = "true", matchIfMissing = true)
public class MensajeriaConfig {

    @Bean
    TopicExchange eventosExchange() {
        return ExchangeBuilder.topicExchange(Eventos.EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(Eventos.DLX).durable(true).build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(Eventos.DLQ).build();
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dlq");
    }

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** Al publicar: copia el correlationId del MDC al header del mensaje. */
    @Bean
    RabbitTemplateCustomizer correlationIdTemplateCustomizer() {
        return template -> template.addBeforePublishPostProcessors(message -> {
            String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (cid != null) {
                message.getMessageProperties().setHeader(CorrelationIdFilter.HEADER, cid);
            }
            return message;
        });
    }

    /** Al consumir: pone el correlationId del header en el MDC para que salga en los logs. */
    @Bean
    ContainerCustomizer<SimpleMessageListenerContainer> correlationIdContainerCustomizer() {
        return container -> container.setAfterReceivePostProcessors(message -> {
            Object cid = message.getMessageProperties().getHeader(CorrelationIdFilter.HEADER);
            if (cid != null) {
                MDC.put(CorrelationIdFilter.MDC_KEY, cid.toString());
            } else {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            }
            return message;
        });
    }

    /** Cola durable cuyos mensajes rechazados (tras los reintentos) van a la DLQ. */
    public static Queue colaConDlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .withArgument("x-dead-letter-exchange", Eventos.DLX)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }
}
