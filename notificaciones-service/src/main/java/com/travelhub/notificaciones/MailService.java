package com.travelhub.notificaciones;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Envia correos HTML. En desarrollo van a Mailpit (http://localhost:8025), nunca a casillas reales. */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final String remitente;

    public MailService(JavaMailSender sender, @Value("${travelhub.mail.remitente}") String remitente) {
        this.sender = sender;
        this.remitente = remitente;
    }

    public void enviar(String para, String asunto, String html) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(remitente);
            h.setTo(para);
            h.setSubject(asunto);
            h.setText(html, true);
            sender.send(msg);
            log.info("Mail enviado a {}: {}", para, asunto);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo armar el mail", e);
        }
    }
}
