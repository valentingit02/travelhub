package com.travelhub.notificaciones;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelhub.common.events.ReservaConfirmadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Pide a ia-service el resumen del viaje y el kit de viaje. Por privacidad solo se envian
 * destino, fechas, productos y el nombre de pila (nunca documento ni email).
 */
@Component
public class IaClient {

    private static final Logger log = LoggerFactory.getLogger(IaClient.class);
    private final RestClient http;

    public IaClient(RestClient.Builder builder, @Value("${travelhub.servicios.ia}") String url) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(2));
        f.setReadTimeout(Duration.ofSeconds(20));
        this.http = builder.clone().baseUrl(url).requestFactory(f).build();
    }

    public record Resumen(String resumen, String generadoPor, Long latenciaMs) { }

    public Resumen resumir(ReservaConfirmadaEvent e) {
        try {
            Map<String, Object> body = Map.of(
                    "nombre", e.nombre(), "destino", e.destino(),
                    "desde", e.desde().toString(), "hasta", e.hasta().toString(),
                    "pasajeros", e.pasajeros(), "items", e.items(),
                    "total", e.total(), "moneda", e.moneda());
            return http.post().uri("/api/ia/resumen").body(body).retrieve().body(Resumen.class);
        } catch (Exception ex) {
            log.warn("ia-service no disponible ({}), se usa resumen basico", ex.getMessage());
            return new Resumen("Tu viaje a " + e.destino() + " del " + e.desde() + " al " + e.hasta()
                    + " esta confirmado. Total: " + e.total() + " " + e.moneda() + ".", "fallback-java", 0L);
        }
    }

    /** Kit de viaje (equipaje, itinerario y libros). Devuelve null si la IA no responde. */
    public JsonNode kit(ReservaConfirmadaEvent e) {
        try {
            return http.get().uri(u -> u.path("/api/ia/kit")
                            .queryParam("destino", e.destino())
                            .queryParam("desde", e.desde().toString())
                            .queryParam("hasta", e.hasta().toString())
                            .queryParam("pax", e.pasajeros())
                            .build())
                    .retrieve().body(JsonNode.class);
        } catch (Exception ex) {
            log.warn("Kit de viaje no disponible: {}", ex.getMessage());
            return null;
        }
    }
}
