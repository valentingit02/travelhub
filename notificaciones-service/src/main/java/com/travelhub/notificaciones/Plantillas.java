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
                      Trabajo Practico · Desarrollo de Aplicaciones II · UADE. Modo demostracion: no se realizan cobros reales.
                    </div>
                  </div>
                </div>
                """.formatted(esc(titulo), cuerpo);
    }

    static String boton(String texto, String url) {
        return "<p><a href=\"" + esc(url) + "\" style=\"display:inline-block;background:#f4b400;color:#0e1b33;"
                + "padding:12px 20px;font-weight:bold;text-decoration:none\">" + esc(texto) + "</a></p>";
    }
}
