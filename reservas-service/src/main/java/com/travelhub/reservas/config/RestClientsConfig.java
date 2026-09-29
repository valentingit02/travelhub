package com.travelhub.reservas.config;

import com.travelhub.common.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientsConfig {

    /** Propaga el correlationId a los otros servicios para seguir un request en todos los logs. */
    private static final ClientHttpRequestInterceptor CORRELATION = (req, body, exec) -> {
        String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (cid != null) req.getHeaders().set(CorrelationIdFilter.HEADER, cid);
        return exec.execute(req, body);
    };

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofSeconds(40)); // Duffel puede tardar en test mode
        return f;
    }

    @Bean
    RestClient catalogoRestClient(RestClient.Builder b, @Value("${travelhub.servicios.catalogo}") String url) {
        return b.clone().baseUrl(url).requestFactory(timeouts()).requestInterceptor(CORRELATION).build();
    }

    @Bean
    RestClient preciosRestClient(RestClient.Builder b, @Value("${travelhub.servicios.precios}") String url) {
        return b.clone().baseUrl(url).requestFactory(timeouts()).requestInterceptor(CORRELATION).build();
    }
}
