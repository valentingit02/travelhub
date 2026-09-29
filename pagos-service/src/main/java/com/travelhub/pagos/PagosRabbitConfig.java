package com.travelhub.pagos;

import com.travelhub.common.events.Eventos;
import com.travelhub.common.messaging.MensajeriaConfig;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PagosRabbitConfig {

    public static final String COLA_PAGO_SOLICITADO = "pagos.pago-solicitado";
    public static final String COLA_REEMBOLSO = "pagos.reembolso-solicitado";

    @Bean Queue colaPagoSolicitado() { return MensajeriaConfig.colaConDlq(COLA_PAGO_SOLICITADO); }
    @Bean Queue colaReembolso() { return MensajeriaConfig.colaConDlq(COLA_REEMBOLSO); }

    @Bean
    Binding bindPagoSolicitado(Queue colaPagoSolicitado, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaPagoSolicitado).to(eventosExchange).with(Eventos.PAGO_SOLICITADO);
    }

    @Bean
    Binding bindReembolso(Queue colaReembolso, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaReembolso).to(eventosExchange).with(Eventos.REEMBOLSO_SOLICITADO);
    }
}
