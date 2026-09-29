package com.travelhub.notificaciones;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelhub.common.events.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.travelhub.notificaciones.Plantillas.*;

/**
 * Cuando se manda cada mail:
 *  - reserva.confirmada -> resumen del viaje (IA) + kit de viaje, al organizador y a sus acompanantes
 *  - reserva.cancelada  -> motivo y aviso de reembolso, a todos los participantes
 *  - invitacion.viaje   -> link para pagar la parte de un viaje compartido
 *  - credito.otorgado   -> proteccion de precio: bajo el precio y se devolvio la diferencia
 *  - alerta.precio      -> se cumplio el precio objetivo de un destino seguido
 */
@Component
public class NotificacionesListeners {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private final MailService mail;
    private final IaClient ia;
    private final String webUrl;

    public NotificacionesListeners(MailService mail, IaClient ia, @Value("${travelhub.web.url}") String webUrl) {
        this.mail = mail;
        this.ia = ia;
        this.webUrl = webUrl;
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_CONFIRMADA)
    public void onConfirmada(ReservaConfirmadaEvent e) {
        IaClient.Resumen resumen = ia.resumir(e);
        String filas = e.items().stream().map(i -> "<tr><td style='padding:6px;border-bottom:1px solid #ebe6da'>"
                + esc(i.tipo()) + "</td><td style='padding:6px;border-bottom:1px solid #ebe6da'>" + esc(i.descripcion())
                + "</td><td style='padding:6px;border-bottom:1px solid #ebe6da;text-align:right'>" + esc(i.precio())
                + "</td></tr>").collect(Collectors.joining());
        String cuerpo = "<p>" + esc(resumen.resumen()).replace("\n", "<br>") + "</p>"
                + "<table style='width:100%;border-collapse:collapse;font-size:14px'>" + filas
                + "<tr><td colspan='2' style='padding:8px 6px'><b>Total</b></td><td style='padding:8px 6px;text-align:right'><b>"
                + esc(e.total()) + " " + esc(e.moneda()) + "</b></td></tr></table>"
                + kitHtml(ia.kit(e))
                + boton("Ver mis viajes", webUrl)
                + "<p style='color:#5f6b7d;font-size:12px'>Reserva #" + e.reservaId() + " · Resumen generado por: "
                + esc(resumen.generadoPor()) + "</p>";
        String asunto = "TravelHub · Tu viaje a " + e.destino() + " está confirmado (#" + e.reservaId() + ")";
        mail.enviar("CONFIRMACION", e.email(), asunto, marco("¡Tu viaje está confirmado!", cuerpo));
        for (String acompanante : nulo(e.acompanantes())) {
            mail.enviar("CONFIRMACION", acompanante, asunto,
                    marco("¡Pagaron todos: " + e.nombre() + " confirmó el viaje!", cuerpo));
        }
    }

    private String kitHtml(JsonNode kit) {
        if (kit == null) return "";
        StringBuilder sb = new StringBuilder("<h3 style='font-family:Georgia,serif;margin:20px 0 6px'>Tu kit de viaje</h3>");
        List<String> equipaje = new ArrayList<>();
        kit.path("equipaje").forEach(x -> equipaje.add(x.path("item").asText()));
        if (!equipaje.isEmpty()) {
            sb.append("<p><b>Qué llevar:</b> ").append(esc(String.join(" · ", equipaje))).append("</p>");
        }
        List<String> libros = new ArrayList<>();
        kit.path("libros").forEach(x -> libros.add("<li><a href=\"" + esc(x.path("url").asText()) + "\">"
                + esc(x.path("titulo").asText()) + "</a> — " + esc(x.path("autor").asText()) + "</li>"));
        if (!libros.isEmpty()) {
            sb.append("<p><b>Para leer en el viaje:</b></p><ul>").append(String.join("", libros.subList(0, Math.min(3, libros.size()))))
                    .append("</ul>");
        }
        return sb.toString();
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_CANCELADA)
    public void onCancelada(ReservaCanceladaEvent e) {
        String estado = e.estado().toLowerCase();
        String cuerpo = "<p>Te avisamos que la reserva <b>#" + e.reservaId() + "</b> quedó <b>" + esc(estado) + "</b>.</p>"
                + "<p>Motivo: " + esc(e.motivo()) + "</p>"
                + "<p>Liberamos todo en los proveedores. Si alguien ya había pagado su parte, se la devolvemos.</p>";
        String asunto = "TravelHub · Reserva #" + e.reservaId() + " " + estado;
        mail.enviar("CANCELACION", e.email(), asunto, marco("Hola " + e.nombre() + ", tu reserva no sigue", cuerpo));
        for (String acompanante : nulo(e.acompanantes())) {
            mail.enviar("CANCELACION", acompanante, asunto, marco("El viaje de " + e.nombre() + " no sigue", cuerpo));
        }
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_INVITACION)
    public void onInvitacion(InvitacionViajeEvent e) {
        String link = webUrl + "/?pagar=" + e.token();
        String cuerpo = "<p><b>" + esc(e.organizador()) + "</b> armó un viaje a <b>" + esc(e.destino()) + "</b> del "
                + esc(e.desde()) + " al " + esc(e.hasta()) + " y te sumó.</p>"
                + "<p>Tu parte: <b style='font-size:20px'>" + esc(e.monto()) + " " + esc(e.moneda()) + "</b></p>"
                + "<p>Tenés tiempo hasta el <b>" + HORA.format(e.venceEn()) + "</b>. Si no pagan todos antes, "
                + "la reserva se cancela y se devuelve lo pagado.</p>"
                + boton("Pagar mi parte", link)
                + "<p style='color:#5f6b7d;font-size:12px'>No hace falta crear una cuenta. Si el botón no funciona, "
                + "copiá este link: " + esc(link) + "</p>";
        mail.enviar("INVITACION", e.email(), "TravelHub · " + e.organizador() + " te invitó a viajar a " + e.destino(),
                marco("¡Te sumaron a un viaje!", cuerpo));
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_CREDITO)
    public void onCredito(CreditoOtorgadoEvent e) {
        String cuerpo = "<p>Después de que reservaste, bajó el precio de <b>" + esc(e.producto()) + "</b> "
                + "(reserva #" + e.reservaId() + ").</p>"
                + "<p>Te devolvemos la diferencia como crédito: <b style='font-size:20px'>" + esc(e.monto()) + " "
                + esc(e.moneda()) + "</b></p>"
                + "<p>Saldo disponible: <b>" + esc(e.saldo()) + " " + esc(e.moneda())
                + "</b>. Se aplica solo en tu próxima reserva.</p>" + boton("Buscar mi próximo viaje", webUrl);
        mail.enviar("CREDITO", e.email(), "TravelHub · Bajó el precio: te devolvimos " + e.monto() + " " + e.moneda(),
                marco("Protección de precio", cuerpo));
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_ALERTA)
    public void onAlerta(AlertaPrecioEvent e) {
        String cuerpo = "<p>Hola " + esc(e.nombre()) + ", <b>" + esc(e.productoRef()) + "</b> ahora cuesta <b>"
                + esc(e.precioNuevo()) + " " + esc(e.moneda()) + "</b> (tu objetivo era " + esc(e.precioObjetivo())
                + " " + esc(e.moneda()) + ").</p>" + boton("Ver disponibilidad", webUrl);
        mail.enviar("ALERTA", e.email(), "TravelHub · Alerta de precio en " + e.destino(),
                marco("¡Bajó el precio en " + e.destino() + "!", cuerpo));
    }

    private static List<String> nulo(List<String> l) {
        return l == null ? List.of() : l;
    }
}
