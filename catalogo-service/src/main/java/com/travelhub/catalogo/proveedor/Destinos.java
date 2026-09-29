package com.travelhub.catalogo.proveedor;

import java.util.Map;

public final class Destinos {

    private Destinos() { }

    public static final Map<String, String> CIUDADES = Map.of(
            "BUE", "Buenos Aires", "AEP", "Buenos Aires", "EZE", "Buenos Aires",
            "BRC", "Bariloche", "MDZ", "Mendoza", "IGR", "Puerto Iguazu",
            "USH", "Ushuaia", "MAD", "Madrid", "SLA", "Salta", "FTE", "El Calafate");

    public static String ciudad(String iata) {
        return iata == null ? "" : CIUDADES.getOrDefault(iata.toUpperCase(), iata.toUpperCase());
    }
}
