package com.travelhub.catalogo.proveedor;

import com.travelhub.common.domain.TipoProducto;
import com.travelhub.common.web.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Proveedor simulado (plan B): datos deterministicos para desarrollar y para la demo
 * sin gastar cuota de las APIs. Incluye un hotel que siempre falla para mostrar la Saga.
 */
@Component
public class MockProveedores {

    private static final Logger log = LoggerFactory.getLogger(MockProveedores.class);
    public static final String HOTEL_FALLA = "MOCK-HOTEL-FALLA";

    private static final Map<String, List<String>> HOTELES = Map.of(
            "BRC", List.of("Llao Llao Resort", "Hotel Catedral", "Hostería del Lago"),
            "MDZ", List.of("Park Hyatt Mendoza", "Hotel Bodega Andina", "Posada Cacheuta"),
            "IGR", List.of("Gran Meliá Iguazú", "Hotel Selva Verde", "Posada Yaguareté"),
            "USH", List.of("Arakur Resort", "Hotel Fin del Mundo", "Hostería Canal Beagle"),
            "MAD", List.of("Hotel Gran Vía", "Hostal Sol", "NH Madrid Centro"));

    private static final Map<String, List<String[]>> EXCURSIONES = Map.of(
            "BRC", List.of(new String[]{"Cerro Catedral", "aventura"}, new String[]{"Circuito Chico", "naturaleza"},
                    new String[]{"Ruta del chocolate", "gastronomia"}),
            "MDZ", List.of(new String[]{"Bodegas de Lujan de Cuyo", "gastronomia"},
                    new String[]{"Alta Montaña y Aconcagua", "naturaleza"}, new String[]{"Rafting en Potrerillos", "aventura"}),
            "IGR", List.of(new String[]{"Cataratas lado argentino", "naturaleza"}, new String[]{"Gran Aventura en lancha", "aventura"},
                    new String[]{"Museo Guemes y selva", "cultura"}),
            "USH", List.of(new String[]{"Tren del Fin del Mundo", "cultura"}, new String[]{"Navegacion Canal Beagle", "naturaleza"},
                    new String[]{"Trekking Laguna Esmeralda", "aventura"}),
            "MAD", List.of(new String[]{"Museo del Prado", "cultura"}, new String[]{"Tour de tapas", "gastronomia"},
                    new String[]{"Toledo dia completo", "cultura"}));

    public List<OfertaProveedor> vuelos(String origen, String destino, LocalDate fecha, int pax) {
        String[] aerolineas = {"Aerolineas Argentinas", "Flybondi", "JetSMART"};
        String[] horas = {"07:15", "12:40", "19:05"};
        int base = destino.equalsIgnoreCase("MAD") ? 850 : 120;
        List<OfertaProveedor> out = new ArrayList<>();
        for (int i = 0; i < aerolineas.length; i++) {
            BigDecimal precio = BigDecimal.valueOf(base + semilla(destino + i) % 90L);
            out.add(new OfertaProveedor("MOCK-VUELO-" + origen + "-" + destino + "-" + i, TipoProducto.VUELO,
                    "MOCK", aerolineas[i] + " " + origen + "-" + destino, destino, precio, "USD", "pasajeros",
                    ocupacion(destino + fecha + i),
                    Map.of("salida", fecha + " " + horas[i], "aerolinea", aerolineas[i])));
        }
        return out;
    }

    public List<OfertaProveedor> hoteles(String destino, LocalDate in, LocalDate out, int pax) {
        List<String> nombres = HOTELES.getOrDefault(destino.toUpperCase(),
                List.of("Hotel Centro", "Hotel Plaza", "Apart Hotel"));
        List<OfertaProveedor> res = new ArrayList<>();
        for (int i = 0; i < nombres.size(); i++) {
            BigDecimal precio = BigDecimal.valueOf(60 + semilla(destino + nombres.get(i)) % 140L);
            res.add(new OfertaProveedor("MOCK-HOTEL-" + destino + "-" + i, TipoProducto.HOTEL, "MOCK",
                    nombres.get(i), destino, precio, "USD", "noches", ocupacion(destino + in + i),
                    Map.of("estrellas", String.valueOf(3 + i % 3), "regimen", i == 0 ? "Desayuno" : "Solo alojamiento")));
        }
        res.add(new OfertaProveedor(HOTEL_FALLA, TipoProducto.HOTEL, "MOCK", "Hotel Falla (demo de Saga)",
                destino, new BigDecimal("80"), "USD", "noches", 0.4, Map.of("nota", "Siempre falla al reservar")));
        return res;
    }

    public List<OfertaProveedor> excursiones(String destino, LocalDate desde, LocalDate hasta) {
        List<String[]> lista = EXCURSIONES.getOrDefault(destino.toUpperCase(),
                List.<String[]>of(new String[]{"City tour", "cultura"}));
        List<OfertaProveedor> res = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            String[] e = lista.get(i);
            BigDecimal precio = BigDecimal.valueOf(25 + semilla(e[0]) % 70L);
            res.add(new OfertaProveedor("MOCK-EXC-" + destino + "-" + i, TipoProducto.EXCURSION, "MOCK", e[0],
                    destino, precio, "USD", "pasajeros", 0.0, Map.of("categoria", e[1], "duracion", (3 + i * 2) + " h")));
        }
        return res;
    }

    public String reservar(String ofertaId) {
        if (HOTEL_FALLA.equals(ofertaId)) {
            log.warn("Mock: el hotel {} rechaza la reserva (demo de compensacion)", ofertaId);
            throw new ExternalServiceException("El proveedor de hoteles rechazo la reserva (sin disponibilidad)");
        }
        String ref = "MOCKREF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Mock: reservado {} -> {}", ofertaId, ref);
        return ref;
    }

    public void cancelar(String ref) {
        log.info("Mock: cancelada la reserva {}", ref);
    }

    private static long semilla(String s) {
        return Math.abs((long) s.hashCode());
    }

    private static double ocupacion(String s) {
        return BigDecimal.valueOf(0.3 + (semilla(s) % 60) / 100.0).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
