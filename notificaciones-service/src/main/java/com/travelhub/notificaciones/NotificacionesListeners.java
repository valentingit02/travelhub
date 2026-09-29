package com.travelhub.notificaciones;

import com.travelhub.common.events.AlertaPrecioEvent;
import com.travelhub.common.events.ReservaCanceladaEvent;
import com.travelhub.common.events.ReservaConfirmadaEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.stream.Collectors;

@Component
public class NotificacionesListeners {

    private final MailService mail;
    private final IaClient ia;

    public NotificacionesListeners(MailService mail, IaClient ia) {
        this.mail = mail;
        this.ia = ia;
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_CONFIRMADA)
    public void onConfirmada(ReservaConfirmadaEvent e) {
        IaClient.Resumen resumen = ia.resumir(e);
        String filas = e.items().stream().map(i -> "<tr><td>" + esc(i.tipo()) + "</td><td>" + esc(i.descripcion())
                + "</td><td style='text-align:right'>" + i.precio() + "</td></tr>").collect(Collectors.joining());
        String html = """
                <h2>¡Tu viaje está confirmado! ✈️</h2>
                <p>%s</p>
                <table border="1" cellpadding="6" style="border-collapse:collapse">
                  <tr><th>Tipo</th><th>Detalle</th><th>Precio</th></tr>%s
                  <tr><td colspan="2"><b>Total</b></td><td style='text-align:right'><b>%s %s</b></td></tr>
                </table>
                <p style="color:#888;font-size:12px">Reserva #%d · Resumen generado por: %s (%d ms)</p>
                """.formatted(esc(resumen.resumen()).replace("\n", "<br>"), filas, e.total(), e.moneda(),
                e.reservaId(), resumen.generadoPor(), resumen.latenciaMs() == null ? 0 : resumen.latenciaMs());
        mail.enviar(e.email(), "TravelHub · Reserva #" + e.reservaId() + " confirmada", html);
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_CANCELADA)
    public void onCancelada(ReservaCanceladaEvent e) {
        String html = """
                <h2>Tu reserva #%d fue %s</h2>
                <p>Hola %s, te avisamos que la reserva quedó en estado <b>%s</b>.</p>
                <p>Motivo: %s</p>
                <p>No se realizó ningún cargo y todo lo reservado fue liberado.</p>
                """.formatted(e.reservaId(), e.estado().toLowerCase(), esc(e.nombre()), e.estado(), esc(e.motivo()));
        mail.enviar(e.email(), "TravelHub · Reserva #" + e.reservaId() + " " + e.estado().toLowerCase(), html);
    }

    @RabbitListener(queues = NotificacionesRabbitConfig.COLA_ALERTA)
    public void onAlerta(AlertaPrecioEvent e) {
        String html = """
                <h2>¡Bajó el precio en %s! 🔔</h2>
                <p>Hola %s, el producto <b>%s</b> ahora cuesta <b>%s %s</b>
                (tu objetivo era %s %s).</p>
                """.formatted(esc(e.destino()), esc(e.nombre()), esc(e.productoRef()), e.precioNuevo(), e.moneda(),
                e.precioObjetivo(), e.moneda());
        mail.enviar(e.email(), "TravelHub · Alerta de precio en " + e.destino(), html);
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
