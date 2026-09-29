package com.travelhub.notificaciones;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Envia correos HTML. Por defecto van a Mailpit (http://localhost:8025): NO llegan a casillas reales.
 * Para mandarlos de verdad, configurar MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD (ver .env.example).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final RegistroEnvios registro;
    private final String remitente;
    private final String host;
    private final int puerto;

    public MailService(JavaMailSender sender, RegistroEnvios registro,
                       @Value("${travelhub.mail.remitente}") String remitente,
                       @Value("${spring.mail.host}") String host,
                       @Value("${spring.mail.port}") int puerto) {
        this.sender = sender;
        this.registro = registro;
        this.remitente = remitente;
        this.host = host;
        this.puerto = puerto;
    }

    @PostConstruct
    void informar() {
        log.info("Mails via SMTP {}:{} ({})", host, puerto, modo());
    }

    public String modo() {
        return host.contains("mailpit") || host.equals("localhost")
                ? "Mailpit: bandeja local en http://localhost:8025, no llega a casillas reales"
                : "SMTP real: llega a la casilla del destinatario";
    }

    public String servidor() {
        return host + ":" + puerto;
    }

    public void enviar(String tipo, String para, String asunto, String html) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(remitente);
            h.setTo(para);
            h.setSubject(asunto);
            h.setText(html, true);
            sender.send(msg);
            registro.registrar(new RegistroEnvios.Envio(Instant.now(), tipo, para, asunto, "ENVIADO", null));
            log.info("Mail [{}] enviado a {}: {}", tipo, para, asunto);
        } catch (MessagingException | MailException e) {
            registro.registrar(new RegistroEnvios.Envio(Instant.now(), tipo, para, asunto, "ERROR", e.getMessage()));
            log.error("No se pudo enviar el mail [{}] a {} via {}: {}", tipo, para, servidor(), e.getMessage());
            throw new IllegalStateException("Fallo el envio del mail a " + para + ": " + e.getMessage(), e);
        }
    }
}
