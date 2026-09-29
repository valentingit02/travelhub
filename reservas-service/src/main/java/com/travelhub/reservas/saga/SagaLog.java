package com.travelhub.reservas.saga;

import jakarta.persistence.*;
import java.time.Instant;

/** Bitacora de cada paso de la Saga: sirve para auditar y para mostrarlo en la demo. */
@Entity
@Table(name = "saga_log")
public class SagaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(nullable = false, length = 40)
    private String paso;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 500)
    private String detalle;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    protected SagaLog() { }

    public SagaLog(Long reservaId, String paso, String estado, String detalle) {
        this.reservaId = reservaId;
        this.paso = paso;
        this.estado = estado;
        this.detalle = detalle != null && detalle.length() > 500 ? detalle.substring(0, 500) : detalle;
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public String getPaso() { return paso; }
    public String getEstado() { return estado; }
    public String getDetalle() { return detalle; }
    public Instant getFecha() { return fecha; }
}
