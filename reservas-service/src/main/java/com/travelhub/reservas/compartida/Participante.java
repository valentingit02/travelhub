package com.travelhub.reservas.compartida;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Cada persona que paga una parte de la reserva. En un viaje individual hay uno solo (el organizador). */
@Entity
@Table(name = "participante")
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(length = 120)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String email;

    /** Link secreto para pagar la parte sin tener cuenta. */
    @Column(nullable = false, unique = true, length = 40)
    private String token = UUID.randomUUID().toString();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoParticipante estado = EstadoParticipante.PENDIENTE;

    @Column(nullable = false)
    private boolean organizador;

    @Column(name = "pago_ref", length = 60)
    private String pagoRef;

    @Column(length = 200)
    private String motivo;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    protected Participante() { }

    public Participante(Long reservaId, String nombre, String email, BigDecimal monto, boolean organizador) {
        this.reservaId = reservaId;
        this.nombre = nombre;
        this.email = email.toLowerCase();
        this.monto = monto;
        this.organizador = organizador;
    }

    public String etiqueta() {
        return (nombre != null && !nombre.isBlank()) ? nombre : email;
    }

    public void setNombre(String nombre) { this.nombre = nombre; }

    private void estado(EstadoParticipante e, String motivo) {
        this.estado = e;
        this.motivo = motivo == null ? null : (motivo.length() > 200 ? motivo.substring(0, 200) : motivo);
        this.actualizadoEn = Instant.now();
    }

    public void marcarProcesando() { estado(EstadoParticipante.PROCESANDO, null); }

    public void marcarPagado(String ref) {
        this.pagoRef = ref;
        estado(EstadoParticipante.PAGADO, null);
    }

    public void marcarRechazado(String motivo) { estado(EstadoParticipante.RECHAZADO, motivo); }

    public void marcarReembolsado(String motivo) { estado(EstadoParticipante.REEMBOLSADO, motivo); }

    public void cancelar() { estado(EstadoParticipante.CANCELADO, null); }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
    public BigDecimal getMonto() { return monto; }
    public EstadoParticipante getEstado() { return estado; }
    public boolean isOrganizador() { return organizador; }
    public String getPagoRef() { return pagoRef; }
    public String getMotivo() { return motivo; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
