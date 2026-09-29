package com.travelhub.catalogo.proveedor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * modo = mock -> todo simulado (no consume cuota ni necesita claves).
 * modo = real -> usa Duffel y Hotelbeds si hay credenciales; si fallan, cae a mock.
 */
@ConfigurationProperties(prefix = "travelhub.proveedores")
public record ProveedoresProperties(String modo, Duffel duffel, Hotelbeds hotelbeds) {

    public boolean real() {
        return "real".equalsIgnoreCase(modo);
    }

    public record Duffel(String url, String token, String version) {
        public boolean configurado() { return token != null && !token.isBlank(); }
    }

    public record Hotelbeds(String url, String hotelKey, String hotelSecret, String activitiesKey,
                            String activitiesSecret, Map<String, String> destinos) {
        public boolean hotelConfigurado() { return hotelKey != null && !hotelKey.isBlank(); }
        public boolean activitiesConfigurado() { return activitiesKey != null && !activitiesKey.isBlank(); }

        /** Codigo de destino de Hotelbeds; por defecto igual al IATA. */
        public String destino(String iata) {
            return destinos != null ? destinos.getOrDefault(iata, iata) : iata;
        }
    }
}
