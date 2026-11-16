package com.travelhub.notificaciones;

import org.springframework.web.util.HtmlUtils;

/** Envoltorio HTML comun de los mails (estilo de la marca, compatible con clientes de correo). */
final class Plantillas {

    private Plantillas() { }

    static String esc(Object s) {
        return s == null ? "" : HtmlUtils.htmlEscape(String.valueOf(s));
    }

    static String marco(String titulo, String cuerpo) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;background:#f6f3ec;padding:24px">
                  <div style="max-width:600px;margin:0 auto;background:#ffffff;border-top:5px solid #f4b400">
                    <div style="background:#0e1b33;color:#ffffff;padding:16px 24px;font-size:20px;font-weight:bold">TravelHub</div>
                    <div style="padding:24px;color:#0e1b33;font-size:15px;line-height:1.5">
                      <h2 style="margin:0 0 12px;font-family:Georgia,serif">%s</h2>
                      %s
                    </div>
                    <div style="padding:12px 24px;color:#5f6b7d;font-size:12px;border-top:1px solid #ebe6da">
                      Trabajo Práctico · Desarrollo de Aplicaciones II · UADE. Modo demostración: no se realizan cobros reales.
                    </div>
                  </div>
                </div>
                """.formatted(esc(titulo), cuerpo);
    }

    private static final java.util.Map<String, String> CIUDADES = java.util.Map.of(
            "BRC", "Bariloche", "MDZ", "Mendoza", "IGR", "Puerto Iguazú", "USH", "Ushuaia", "MAD", "Madrid");

    /** Nombre de la ciudad para mostrar ("BRC" -> "Bariloche"); si no la conoce, devuelve el codigo. */
    static String ciudad(String iata) {
        return iata == null ? "" : CIUDADES.getOrDefault(iata.toUpperCase(), iata);
    }

    /** Tipo de producto legible ("EXCURSION" -> "Excursión"). */
    static String tipo(Object t) {
        String s = String.valueOf(t);
        return switch (s) {
            case "VUELO" -> "Vuelo";
            case "HOTEL" -> "Alojamiento";
            case "AUTO" -> "Auto";
            case "EXCURSION" -> "Excursión";
            default -> s;
        };
    }

    /** Fecha "2026-12-31" -> "31/12/2026". */
    static String fecha(Object iso) {
        String s = String.valueOf(iso);
        return s.matches("\\d{4}-\\d{2}-\\d{2}.*") ? s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4) : s;
    }

    static String boton(String texto, String url) {
        return "<p><a href=\"" + esc(url) + "\" style=\"display:inline-block;background:#f4b400;color:#0e1b33;"
                + "padding:12px 20px;font-weight:bold;text-decoration:none\">" + esc(texto) + "</a></p>";
    }
}
