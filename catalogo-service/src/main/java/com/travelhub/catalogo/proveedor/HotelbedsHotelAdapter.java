package com.travelhub.catalogo.proveedor;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.util.HotelbedsSignature;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Patron Adapter para Hotelbeds Hotel API (entorno de evaluacion, 50 requests/dia).
 * La cache de Redis (@Cacheable) evita gastar la cuota con busquedas repetidas.
 */
@Component
public class HotelbedsHotelAdapter implements ProveedorHoteles {

    private static final Logger log = LoggerFactory.getLogger(HotelbedsHotelAdapter.class);

    private final ProveedoresProperties props;
    private final MockProveedores mock;
    private final RestClient http;

    public HotelbedsHotelAdapter(ProveedoresProperties props, MockProveedores mock, RestClient.Builder builder) {
        this.props = props;
        this.mock = mock;
        this.http = builder.clone()
                .baseUrl(props.hotelbeds().url())
                .requestFactory(HttpClients.conTimeouts(Duration.ofSeconds(5), Duration.ofSeconds(20)))
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private boolean activo() {
        return props.real() && props.hotelbeds().hotelConfigurado();
    }

    private String firma() {
        return HotelbedsSignature.firmarAhora(props.hotelbeds().hotelKey(), props.hotelbeds().hotelSecret());
    }

    @Override
    @Cacheable(cacheNames = "hoteles")
    public List<OfertaProveedor> buscar(String destino, LocalDate checkIn, LocalDate checkOut, int pasajeros) {
        if (!activo()) return mock.hoteles(destino, checkIn, checkOut, pasajeros);
        try {
            Map<String, Object> body = Map.of(
                    "stay", Map.of("checkIn", checkIn.toString(), "checkOut", checkOut.toString()),
                    "occupancies", List.of(Map.of("rooms", 1, "adults", pasajeros, "children", 0)),
                    "destination", Map.of("code", props.hotelbeds().destino(destino)));
            JsonNode resp = http.post().uri("/hotel-api/1.0/hotels")
                    .header("Api-key", props.hotelbeds().hotelKey()).header("X-Signature", firma())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);

            long noches = Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
            List<OfertaProveedor> out = new ArrayList<>();
            for (JsonNode h : resp.path("hotels").path("hotels")) {
                if (out.size() == 5) break;
                JsonNode rate = h.path("rooms").path(0).path("rates").path(0);
                BigDecimal total = new BigDecimal(rate.path("net").asText(h.path("minRate").asText("0")));
                out.add(new OfertaProveedor(rate.path("rateKey").asText(), TipoProducto.HOTEL, "HOTELBEDS",
                        h.path("name").asText(), destino,
                        total.divide(BigDecimal.valueOf(noches), 2, RoundingMode.HALF_UP),
                        h.path("currency").asText("EUR"), "noches", 0.5,
                        Map.of("categoria", h.path("categoryName").asText(""),
                                "habitacion", h.path("rooms").path(0).path("name").asText(""),
                                "rateType", rate.path("rateType").asText(""))));
            }
            log.info("Hotelbeds devolvio {} hoteles en {}", out.size(), destino);
            out.add(mock.hoteles(destino, checkIn, checkOut, pasajeros).getLast()); // hotel de demo de Saga
            return out.size() == 1 ? mock.hoteles(destino, checkIn, checkOut, pasajeros) : out;
        } catch (Exception e) {
            log.warn("Hotelbeds no disponible ({}), uso datos simulados", e.getMessage());
            return mock.hoteles(destino, checkIn, checkOut, pasajeros);
        }
    }

    @Override
    public String reservar(String ofertaId, Titular t, int pasajeros, String reservaRef) {
        if (ofertaId.startsWith("MOCK-")) return mock.reservar(ofertaId);
        try {
            List<Map<String, Object>> paxes = new ArrayList<>();
            for (int i = 0; i < pasajeros; i++) {
                paxes.add(Map.of("roomId", 1, "type", "AD",
                        "name", i == 0 ? t.nombre() : "Acompanante", "surname", t.apellido()));
            }
            Map<String, Object> body = Map.of(
                    "holder", Map.of("name", t.nombre(), "surname", t.apellido()),
                    "rooms", List.of(Map.of("rateKey", ofertaId, "paxes", paxes)),
                    "clientReference", reservaRef,
                    "remark", "Reserva de prueba TravelHub (TP UADE)",
                    "tolerance", 2);
            String ref = http.post().uri("/hotel-api/1.0/bookings")
                    .header("Api-key", props.hotelbeds().hotelKey()).header("X-Signature", firma())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class)
                    .path("booking").path("reference").asText();
            log.info("Hotelbeds: reserva {} creada para {}", ref, reservaRef);
            return ref;
        } catch (Exception e) {
            throw new ExternalServiceException("Hotelbeds rechazo la reserva del hotel: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelar(String refExterna) {
        if (refExterna.startsWith("MOCKREF-")) {
            mock.cancelar(refExterna);
            return;
        }
        try {
            http.delete().uri("/hotel-api/1.0/bookings/{ref}?cancellationFlag=CANCELLATION", refExterna)
                    .header("Api-key", props.hotelbeds().hotelKey()).header("X-Signature", firma())
                    .retrieve().toBodilessEntity();
            log.info("Hotelbeds: reserva {} cancelada", refExterna);
        } catch (Exception e) {
            throw new ExternalServiceException("No se pudo cancelar la reserva de Hotelbeds " + refExterna, e);
        }
    }
}
