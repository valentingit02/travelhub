package com.travelhub.notificaciones;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Diagnostico de mails")
public class NotificacionController {

    private final MailService mail;
    private final RegistroEnvios registro;

    public NotificacionController(MailService mail, RegistroEnvios registro) {
        this.mail = mail;
        this.registro = registro;
    }

    public record Estado(String servidor, String modo, List<RegistroEnvios.Envio> envios) { }

    @GetMapping
    @Operation(summary = "A donde se mandan los mails y los ultimos 50 envios (OK o ERROR)")
    public Estado estado() {
        return new Estado(mail.servidor(), mail.modo(), registro.ultimos());
    }

    @PostMapping("/prueba")
    @Operation(summary = "Manda un mail de prueba en el momento")
    public ResponseEntity<Map<String, String>> prueba(@RequestParam String email) {
        try {
            mail.enviar("PRUEBA", email, "TravelHub · Mail de prueba",
                    Plantillas.marco("Funciona", "<p>Si ves esto, los mails de TravelHub salen bien por "
                            + Plantillas.esc(mail.servidor()) + ".</p>"));
            return ResponseEntity.ok(Map.of("resultado", "ENVIADO", "servidor", mail.servidor(), "modo", mail.modo()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(502).body(Map.of("resultado", "ERROR", "servidor", mail.servidor(),
                    "detalle", e.getMessage()));
        }
    }
}
