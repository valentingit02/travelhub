package com.travelhub.notificaciones;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Ultimos 50 envios (en memoria) para diagnosticar si un mail salio o fallo. */
@Component
public class RegistroEnvios {

    public record Envio(Instant fecha, String tipo, String para, String asunto, String estado, String error) { }

    private static final int MAX = 50;
    private final ConcurrentLinkedDeque<Envio> envios = new ConcurrentLinkedDeque<>();

    public void registrar(Envio e) {
        envios.addFirst(e);
        while (envios.size() > MAX) envios.pollLast();
    }

    public List<Envio> ultimos() {
        return new ArrayList<>(envios);
    }
}
