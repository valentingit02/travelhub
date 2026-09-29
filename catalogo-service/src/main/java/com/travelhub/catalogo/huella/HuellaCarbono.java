package com.travelhub.catalogo.huella;

import java.util.Map;
import java.util.OptionalInt;

/**
 * Estimacion simplificada de CO2 por pasajero para un tramo de vuelo:
 * distancia ortodromica x 1.08 (desvios) x factor por km segun la distancia del tramo.
 * Los factores son aproximados: documentar la fuente usada en el informe.
 */
public final class HuellaCarbono {

    private HuellaCarbono() { }

    private static final Map<String, double[]> COORDENADAS = Map.ofEntries(
            Map.entry("AEP", new double[]{-34.5592, -58.4156}),
            Map.entry("EZE", new double[]{-34.8222, -58.5358}),
            Map.entry("COR", new double[]{-31.3236, -64.2080}),
            Map.entry("ROS", new double[]{-32.9036, -60.7850}),
            Map.entry("BRC", new double[]{-41.1512, -71.1578}),
            Map.entry("MDZ", new double[]{-32.8317, -68.7929}),
            Map.entry("IGR", new double[]{-25.7373, -54.4734}),
            Map.entry("USH", new double[]{-54.8433, -68.2958}),
            Map.entry("SLA", new double[]{-24.8560, -65.4862}),
            Map.entry("FTE", new double[]{-50.2803, -72.0531}),
            Map.entry("MAD", new double[]{40.4719, -3.5626}));

    public static OptionalInt kilometros(String origen, String destino) {
        double[] a = COORDENADAS.get(origen == null ? "" : origen.toUpperCase());
        double[] b = COORDENADAS.get(destino == null ? "" : destino.toUpperCase());
        if (a == null || b == null) return OptionalInt.empty();
        double lat1 = Math.toRadians(a[0]), lat2 = Math.toRadians(b[0]);
        double dLat = lat2 - lat1, dLon = Math.toRadians(b[1] - a[1]);
        double h = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        return OptionalInt.of((int) Math.round(6371 * 2 * Math.asin(Math.sqrt(h))));
    }

    /** kg de CO2 por pasajero para el tramo, o vacio si no se conocen los aeropuertos. */
    public static OptionalInt kgPorPasajero(String origen, String destino) {
        OptionalInt km = kilometros(origen, destino);
        if (km.isEmpty()) return OptionalInt.empty();
        int d = km.getAsInt();
        double factor = d < 1500 ? 0.15 : d < 4000 ? 0.12 : 0.10;   // kg CO2 por pasajero-km (aprox.)
        return OptionalInt.of((int) Math.round(d * 1.08 * factor));
    }
}
