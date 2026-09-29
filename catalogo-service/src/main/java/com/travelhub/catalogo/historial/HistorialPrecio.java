package com.travelhub.catalogo.historial;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Historico de precios: sirve de dataset para el detector de anomalias. */
@Entity
@Table(name = "historial_precio")
public class HistorialPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_producto", nullable = false, length = 20)
    private String tipoProducto;

    @Column(name = "producto_ref", nullable = false, length = 80)
    private String productoRef;

    @Column(name = "destino_iata", length = 3)
    private String destinoIata;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro = Instant.now();

    protected HistorialPrecio() { }

    public HistorialPrecio(String tipoProducto, String productoRef, String destinoIata, BigDecimal precio, String moneda) {
        this.tipoProducto = tipoProducto;
        this.productoRef = productoRef;
        this.destinoIata = destinoIata;
        this.precio = precio;
        this.moneda = moneda;
    }

    public Long getId() { return id; }
    public String getTipoProducto() { return tipoProducto; }
    public String getProductoRef() { return productoRef; }
    public String getDestinoIata() { return destinoIata; }
    public BigDecimal getPrecio() { return precio; }
    public String getMoneda() { return moneda; }
    public Instant getFechaRegistro() { return fechaRegistro; }
}
