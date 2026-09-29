package com.travelhub.reservas.credito;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movimiento_credito")
public class MovimientoCredito {

    public enum Tipo { OTORGADO, USO, DEVOLUCION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viajero_id", nullable = false)
    private Long viajeroId;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "item_id")
    private Long itemId;

    /** Positivo suma credito, negativo lo consume. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(length = 300)
    private String motivo;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    protected MovimientoCredito() { }

    public MovimientoCredito(Long viajeroId, Long reservaId, Long itemId, BigDecimal monto, String moneda,
                             Tipo tipo, String motivo) {
        this.viajeroId = viajeroId;
        this.reservaId = reservaId;
        this.itemId = itemId;
        this.monto = monto;
        this.moneda = moneda;
        this.tipo = tipo;
        this.motivo = motivo == null ? null : (motivo.length() > 300 ? motivo.substring(0, 300) : motivo);
    }

    public Long getId() { return id; }
    public Long getViajeroId() { return viajeroId; }
    public Long getReservaId() { return reservaId; }
    public Long getItemId() { return itemId; }
    public BigDecimal getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public Tipo getTipo() { return tipo; }
    public String getMotivo() { return motivo; }
    public Instant getFecha() { return fecha; }
}
