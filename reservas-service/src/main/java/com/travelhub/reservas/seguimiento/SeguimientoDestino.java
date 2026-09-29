package com.travelhub.reservas.seguimiento;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** El viajero "sigue" un destino: si aparece un precio menor o igual al objetivo, se le avisa. */
@Entity
@Table(name = "seguimiento_destino")
public class SeguimientoDestino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viajero_id", nullable = false)
    private Long viajeroId;

    @Column(nullable = false, length = 3)
    private String destino;

    @Column(name = "tipo_producto", nullable = false, length = 20)
    private String tipoProducto;

    @Column(name = "precio_objetivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioObjetivo;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false)
    private boolean activo = true;

    protected SeguimientoDestino() { }

    public SeguimientoDestino(Long viajeroId, String destino, String tipoProducto, BigDecimal precioObjetivo, String moneda) {
        this.viajeroId = viajeroId;
        this.destino = destino;
        this.tipoProducto = tipoProducto;
        this.precioObjetivo = precioObjetivo;
        this.moneda = moneda;
    }

    public void desactivar() { this.activo = false; }

    public Long getId() { return id; }
    public Long getViajeroId() { return viajeroId; }
    public String getDestino() { return destino; }
    public String getTipoProducto() { return tipoProducto; }
    public BigDecimal getPrecioObjetivo() { return precioObjetivo; }
    public String getMoneda() { return moneda; }
    public boolean isActivo() { return activo; }
}
