package com.travelhub.catalogo.auto;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Disponibilidad: un auto reservado entre dos fechas por una reserva. */
@Entity
@Table(name = "bloqueo_auto", indexes = @Index(name = "ix_bloqueo_reserva", columnList = "reserva_ref"))
public class BloqueoAuto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auto_id", nullable = false)
    private Long autoId;

    @Column(nullable = false)
    private LocalDate desde;

    @Column(nullable = false)
    private LocalDate hasta;

    @Column(name = "reserva_ref", nullable = false, length = 40)
    private String reservaRef;

    protected BloqueoAuto() { }

    public BloqueoAuto(Long autoId, LocalDate desde, LocalDate hasta, String reservaRef) {
        this.autoId = autoId;
        this.desde = desde;
        this.hasta = hasta;
        this.reservaRef = reservaRef;
    }

    public Long getId() { return id; }
    public Long getAutoId() { return autoId; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public String getReservaRef() { return reservaRef; }
}
