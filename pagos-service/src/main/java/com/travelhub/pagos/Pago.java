package com.travelhub.pagos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pago")
public class Pago {

    public enum Estado { APROBADO, RECHAZADO, REEMBOLSADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "participante_id")
    private Long participanteId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false, length = 30)
    private String proveedor;

    @Column(name = "ref_proveedor", length = 60)
    private String refProveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    protected Pago() { }

    public Pago(Long reservaId, Long participanteId, BigDecimal monto, String moneda, String proveedor,
                String refProveedor, Estado estado, String motivo) {
        this.reservaId = reservaId;
        this.participanteId = participanteId;
        this.monto = monto;
        this.moneda = moneda;
        this.proveedor = proveedor;
        this.refProveedor = refProveedor;
        this.estado = estado;
        this.motivo = motivo;
    }

    public void reembolsar(String motivo) {
        this.estado = Estado.REEMBOLSADO;
        this.motivo = motivo == null ? null : (motivo.length() > 200 ? motivo.substring(0, 200) : motivo);
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public Long getParticipanteId() { return participanteId; }
    public BigDecimal getMonto() { return monto; }
    public String getMoneda() { return moneda; }
    public String getProveedor() { return proveedor; }
    public String getRefProveedor() { return refProveedor; }
    public Estado getEstado() { return estado; }
    public String getMotivo() { return motivo; }
    public Instant getFecha() { return fecha; }
}
