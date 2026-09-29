package com.travelhub.reservas.outbox;

import jakarta.persistence.*;
import java.time.Instant;

/** Evento pendiente de publicar (patron Transactional Outbox). */
@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routing_key", nullable = false, length = 80)
    private String routingKey;

    @Column(nullable = false, length = 200)
    private String tipo;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "publicado_en")
    private Instant publicadoEn;

    @Column(nullable = false)
    private int intentos;

    protected OutboxEvento() { }

    public OutboxEvento(String routingKey, String tipo, String payload, String correlationId) {
        this.routingKey = routingKey;
        this.tipo = tipo;
        this.payload = payload;
        this.correlationId = correlationId;
    }

    public void marcarPublicado() {
        this.publicadoEn = Instant.now();
        this.intentos++;
    }

    public void registrarFallo() {
        this.intentos++;
    }

    public Long getId() { return id; }
    public String getRoutingKey() { return routingKey; }
    public String getTipo() { return tipo; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getPublicadoEn() { return publicadoEn; }
    public int getIntentos() { return intentos; }
}
