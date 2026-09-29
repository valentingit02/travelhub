package com.travelhub.reservas.reserva;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Pedido del enunciado: la reserva de un paquete (individual o compartida entre amigos). */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viajero_id", nullable = false)
    private Long viajeroId;

    @Column(nullable = false, length = 3)
    private String destino;

    @Column(nullable = false)
    private LocalDate desde;

    @Column(nullable = false)
    private LocalDate hasta;

    @Column(nullable = false)
    private Integer pasajeros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 3)
    private String moneda;

    @Column(length = 300)
    private String motivo;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn = Instant.now();

    @Column(name = "idempotency_key", length = 80, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private boolean compartida;

    /** Si sigue PENDIENTE despues de esto, se compensa y pasa a FALLIDA. */
    @Column(name = "vence_en", nullable = false)
    private Instant venceEn = creadaEn.plus(Duration.ofMinutes(15));

    @Column(name = "credito_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditoAplicado = BigDecimal.ZERO;

    // EAGER a proposito: la Saga trabaja fuera de una transaccion larga
    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<ItemReserva> items = new ArrayList<>();

    protected Reserva() { }

    public Reserva(Long viajeroId, String destino, LocalDate desde, LocalDate hasta, Integer pasajeros) {
        this.viajeroId = viajeroId;
        this.destino = destino;
        this.desde = desde;
        this.hasta = hasta;
        this.pasajeros = pasajeros;
    }

    public void agregarItem(ItemReserva item) {
        item.setReserva(this);
        items.add(item);
    }

    public void setTotal(BigDecimal total, String moneda) {
        this.total = total;
        this.moneda = moneda;
    }

    public void asignarClaveIdempotencia(String clave) {
        this.idempotencyKey = clave;
    }

    public void configurarPago(boolean compartida, Duration plazo) {
        this.compartida = compartida;
        this.venceEn = creadaEn.plus(plazo);
    }

    public void aplicarCredito(BigDecimal monto) {
        this.creditoAplicado = monto;
    }

    public void cambiarEstado(EstadoReserva nuevo, String motivo) {
        this.estado = nuevo;
        if (motivo != null) this.motivo = motivo.length() > 300 ? motivo.substring(0, 300) : motivo;
    }

    public String referencia() { return "RES-" + id; }

    public Long getId() { return id; }
    public Long getViajeroId() { return viajeroId; }
    public String getDestino() { return destino; }
    public LocalDate getDesde() { return desde; }
    public LocalDate getHasta() { return hasta; }
    public Integer getPasajeros() { return pasajeros; }
    public EstadoReserva getEstado() { return estado; }
    public BigDecimal getTotal() { return total; }
    public String getMoneda() { return moneda; }
    public String getMotivo() { return motivo; }
    public Instant getCreadaEn() { return creadaEn; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public boolean isCompartida() { return compartida; }
    public Instant getVenceEn() { return venceEn; }
    public BigDecimal getCreditoAplicado() { return creditoAplicado; }
    public List<ItemReserva> getItems() { return items; }
}
