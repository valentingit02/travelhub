package com.travelhub.catalogo.proveedor;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * Patron Adapter: traduce la API de Duffel (vuelos, test mode) al modelo de TravelHub.
 * Si no hay token o Duffel falla, la busqueda cae al proveedor simulado.
 */
@Component
public class DuffelAdapter implements ProveedorVuelos {

    private static final Logger log = LoggerFactory.getLogger(DuffelAdapter.class);

    private final ProveedoresProperties props;
    private final MockProveedores mock;
    private final RestClient http;

    public DuffelAdapter(ProveedoresProperties props, MockProveedores mock, RestClient.Builder builder) {
        this.props = props;
        this.mock = mock;
        this.http = builder.clone()
                .baseUrl(props.duffel().url())
                .requestFactory(HttpClients.conTimeouts(Duration.ofSeconds(5), Duration.ofSeconds(25)))
                .defaultHeader("Authorization", "Bearer " + props.duffel().token())
                .defaultHeader("Duffel-Version", props.duffel().version())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private boolean activo() {
        return props.real() && props.duffel().configurado();
    }

    @Override
    @Cacheable(cacheNames = "vuelos")
    public List<OfertaProveedor> buscar(String origen, String destino, LocalDate fecha, int pasajeros) {
        if (!activo()) return mock.vuelos(origen, destino, fecha, pasajeros);
        try {
            List<Map<String, String>> pax = new ArrayList<>();
            for (int i = 0; i < pasajeros; i++) pax.add(Map.of("type", "adult"));
            Map<String, Object> body = Map.of("data", Map.of(
                    "slices", List.of(Map.of("origin", origen, "destination", destino, "departure_date", fecha.toString())),
                    "passengers", pax,
                    "cabin_class", "economy"));

            JsonNode resp = http.post().uri("/air/offer_requests?return_offers=true&supplier_timeout=10000")
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);

            List<OfertaProveedor> out = new ArrayList<>();
            for (JsonNode o : resp.path("data").path("offers")) {
                if (out.size() == 5) break;
                BigDecimal total = new BigDecimal(o.path("total_amount").asText("0"));
                BigDecimal porPersona = total.divide(BigDecimal.valueOf(pasajeros), 2, RoundingMode.HALF_UP);
                JsonNode seg = o.path("slices").path(0).path("segments").path(0);
                String aerolinea = o.path("owner").path("name").asText("Aerolinea");
                out.add(new OfertaProveedor(o.path("id").asText(), TipoProducto.VUELO, "DUFFEL",
                        aerolinea + " " + origen + "-" + destino, destino, porPersona,
                        o.path("total_currency").asText("USD"), "pasajeros", 0.5,
                        Map.of("salida", seg.path("departing_at").asText(""), "aerolinea", aerolinea)));
            }
            log.info("Duffel devolvio {} ofertas {}-{} {}", out.size(), origen, destino, fecha);
            return out.isEmpty() ? mock.vuelos(origen, destino, fecha, pasajeros) : out;
        } catch (Exception e) {
            log.warn("Duffel no disponible ({}), uso datos simulados", e.getMessage());
            return mock.vuelos(origen, destino, fecha, pasajeros);
        }
    }

    @Override
    public String reservar(String ofertaId, Titular t, int pasajeros, String reservaRef) {
        if (ofertaId.startsWith("MOCK-")) return mock.reservar(ofertaId);
        try {
            JsonNode oferta = http.get().uri("/air/offers/{id}", ofertaId).retrieve().body(JsonNode.class).path("data");
            List<Map<String, Object>> pax = new ArrayList<>();
            int i = 0;
            for (JsonNode p : oferta.path("passengers")) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.path("id").asText());
                m.put("title", "mr");
                m.put("gender", "m");
                m.put("given_name", i == 0 ? t.nombre() : "Acompanante");
                m.put("family_name", t.apellido());
                m.put("born_on", t.fechaNacimiento().toString());
                m.put("email", t.email());
                m.put("phone_number", "+541155550000");
                pax.add(m);
                i++;
            }
            Map<String, Object> body = Map.of("data", Map.of(
                    "type", "instant",
                    "selected_offers", List.of(ofertaId),
                    "payments", List.of(Map.of("type", "balance",
                            "currency", oferta.path("total_currency").asText(),
                            "amount", oferta.path("total_amount").asText())),
                    "passengers", pax,
                    "metadata", Map.of("reserva", reservaRef)));
            String orderId = http.post().uri("/air/orders").contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(JsonNode.class).path("data").path("id").asText();
            log.info("Duffel: orden {} creada para {}", orderId, reservaRef);
            return orderId;
        } catch (Exception e) {
            throw new ExternalServiceException("Duffel rechazo la reserva del vuelo: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelar(String refExterna) {
        if (refExterna.startsWith("MOCKREF-")) {
            mock.cancelar(refExterna);
            return;
        }
        try {
            String cancelId = http.post().uri("/air/order_cancellations").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("data", Map.of("order_id", refExterna)))
                    .retrieve().body(JsonNode.class).path("data").path("id").asText();
            http.post().uri("/air/order_cancellations/{id}/actions/confirm", cancelId).retrieve().toBodilessEntity();
            log.info("Duffel: orden {} cancelada", refExterna);
        } catch (Exception e) {
            throw new ExternalServiceException("No se pudo cancelar la orden de Duffel " + refExterna, e);
        }
    }
}
