package com.travelhub.catalogo.proveedor;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.util.HotelbedsSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * Adapter para Hotelbeds Activities API (busqueda real de excursiones).
 * La reserva de la excursion se registra internamente: el flujo de booking de Activities
 * (detalle + confirmacion por modalidad) no aporta al TP y consume cuota.
 */
@Component
public class HotelbedsActivitiesAdapter implements ProveedorExcursiones {

    private static final Logger log = LoggerFactory.getLogger(HotelbedsActivitiesAdapter.class);

    private final ProveedoresProperties props;
    private final MockProveedores mock;
    private final RestClient http;

    public HotelbedsActivitiesAdapter(ProveedoresProperties props, MockProveedores mock, RestClient.Builder builder) {
        this.props = props;
        this.mock = mock;
        this.http = builder.clone()
                .baseUrl(props.hotelbeds().url())
                .requestFactory(HttpClients.conTimeouts(Duration.ofSeconds(5), Duration.ofSeconds(20)))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private boolean activo() {
        return props.real() && props.hotelbeds().activitiesConfigurado();
    }

    @Override
    @Cacheable(cacheNames = "excursiones")
    public List<OfertaProveedor> buscar(String destino, LocalDate desde, LocalDate hasta) {
        if (!activo()) return mock.excursiones(destino, desde, hasta);
        try {
            Map<String, Object> filtro = Map.of("type", "destination", "value", props.hotelbeds().destino(destino));
            Map<String, Object> body = Map.of(
                    "filters", List.of(Map.of("searchFilterItems", List.of(filtro))),
                    "from", desde.toString(),
                    "to", hasta.toString(),
                    "language", "es",
                    "pagination", Map.of("itemsPerPage", 10, "page", 1),
                    "order", "DEFAULT");
            JsonNode resp = http.post().uri("/activity-api/3.0/activities")
                    .header("Api-key", props.hotelbeds().activitiesKey())
                    .header("X-Signature", HotelbedsSignature.firmarAhora(props.hotelbeds().activitiesKey(),
                            props.hotelbeds().activitiesSecret()))
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);

            List<OfertaProveedor> out = new ArrayList<>();
            for (JsonNode a : resp.path("activities")) {
                if (out.size() == 5) break;
                String precio = a.path("amountsFrom").path(0).path("amount").asText("0");
                out.add(new OfertaProveedor("HBACT-" + a.path("code").asText(), TipoProducto.EXCURSION, "HOTELBEDS",
                        a.path("name").asText(), destino, new BigDecimal(precio), a.path("currency").asText("EUR"),
                        "pasajeros", 0.0, Map.of("codigo", a.path("code").asText())));
            }
            log.info("Hotelbeds Activities devolvio {} excursiones en {}", out.size(), destino);
            return out.isEmpty() ? mock.excursiones(destino, desde, hasta) : out;
        } catch (Exception e) {
            log.warn("Hotelbeds Activities no disponible ({}), uso datos simulados", e.getMessage());
            return mock.excursiones(destino, desde, hasta);
        }
    }

    @Override
    public String reservar(String ofertaId, Titular titular, int pasajeros, String reservaRef) {
        return mock.reservar(ofertaId);
    }

    @Override
    public void cancelar(String refExterna) {
        mock.cancelar(refExterna);
    }
}
