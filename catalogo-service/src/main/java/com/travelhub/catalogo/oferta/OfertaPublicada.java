package com.travelhub.catalogo.oferta;

import com.travelhub.catalogo.proveedor.OfertaProveedor;
import com.travelhub.common.domain.TipoProducto;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Oferta mostrada en una busqueda, con su precio en USD. Fuente de verdad para reservar. */
@Entity
@Table(name = "oferta_publicada")
public class OfertaPublicada {

    @Id
    @Column(length = 1000)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProducto tipo;

    @Column(nullable = false, length = 30)
    private String proveedor;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 3)
    private String destino;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false)
    private double ocupacion;

    @Column(nullable = false)
    private boolean bloqueada;

    @Column(name = "motivo_bloqueo", length = 200)
    private String motivoBloqueo;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    protected OfertaPublicada() { }

    public OfertaPublicada(OfertaProveedor o, Instant expiraEn) {
        this.id = o.id();
        refrescar(o, expiraEn);
    }

    public void refrescar(OfertaProveedor o, Instant expira) {
        this.tipo = o.tipo();
        this.proveedor = recortar(o.proveedor(), 30);
        this.nombre = recortar(o.nombre(), 200);
        this.destino = o.destino().toUpperCase();
        this.precio = o.precioBase();
        this.moneda = o.moneda();
        this.ocupacion = o.ocupacion();
        this.actualizadaEn = Instant.now();
        this.expiraEn = expira;
    }

    public void bloquear(String motivo) {
        this.bloqueada = true;
        this.motivoBloqueo = recortar(motivo, 200);
    }

    public void desbloquear() {
        this.bloqueada = false;
        this.motivoBloqueo = null;
    }

    public boolean vencida() {
        return expiraEn.isBefore(Instant.now());
    }

    private static String recortar(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    public String getId() { return id; }
    public TipoProducto getTipo() { return tipo; }
    public String getProveedor() { return proveedor; }
    public String getNombre() { return nombre; }
    public String getDestino() { return destino; }
    public BigDecimal getPrecio() { return precio; }
    public String getMoneda() { return moneda; }
    public double getOcupacion() { return ocupacion; }
    public boolean isBloqueada() { return bloqueada; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public Instant getActualizadaEn() { return actualizadaEn; }
    public Instant getExpiraEn() { return expiraEn; }
}
