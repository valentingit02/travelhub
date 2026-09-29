package com.travelhub.reservas.reserva;

import com.travelhub.common.domain.TipoProducto;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "item_reserva")
public class ItemReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProducto tipo;

    @Column(name = "producto_id", nullable = false, length = 1000)   // los rateKey de Hotelbeds son largos
    private String productoId;

    @Column(nullable = false, length = 200)
    private String descripcion;

    /** Precio base VERIFICADO por el catalogo (USD), nunca el que manda el cliente. */
    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "precio_cotizado", precision = 12, scale = 2)
    private BigDecimal precioCotizado;

    @Column(name = "ref_externa", length = 80)
    private String refExterna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoItem estado = EstadoItem.PENDIENTE;

    protected ItemReserva() { }

    public ItemReserva(TipoProducto tipo, String productoId, String descripcion, BigDecimal precioBase) {
        this.tipo = tipo;
        this.productoId = productoId;
        this.descripcion = descripcion.length() > 200 ? descripcion.substring(0, 200) : descripcion;
        this.precioBase = precioBase;
    }

    void setReserva(Reserva reserva) { this.reserva = reserva; }

    public void setPrecioCotizado(BigDecimal p) { this.precioCotizado = p; }

    public void marcarReservado(String ref) {
        this.refExterna = ref;
        this.estado = EstadoItem.RESERVADO;
    }

    public void marcarFallido() { this.estado = EstadoItem.FALLIDO; }

    public void marcarCancelado() { this.estado = EstadoItem.CANCELADO; }

    public Long getId() { return id; }
    public TipoProducto getTipo() { return tipo; }
    public String getProductoId() { return productoId; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecioBase() { return precioBase; }
    public BigDecimal getPrecioCotizado() { return precioCotizado; }
    public String getRefExterna() { return refExterna; }
    public EstadoItem getEstado() { return estado; }
}
