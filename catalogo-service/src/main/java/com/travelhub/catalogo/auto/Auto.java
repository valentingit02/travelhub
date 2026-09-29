package com.travelhub.catalogo.auto;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Inventario propio de autos de alquiler (capa de datos). */
@Entity
@Table(name = "auto")
public class Auto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destino_iata", nullable = false, length = 3)
    private String destinoIata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaAuto categoria;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 60)
    private String modelo;

    @Column(nullable = false)
    private Integer plazas;

    @Column(name = "transmision_automatica", nullable = false)
    private boolean transmisionAutomatica;

    @Column(name = "precio_base_dia", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBaseDia;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false)
    private boolean activo = true;

    /** true cuando la IA detecto un precio anomalo: no se publica hasta revisarlo. */
    @Column(name = "en_revision", nullable = false)
    private boolean enRevision = false;

    protected Auto() { } // requerido por JPA

    public Auto(String destinoIata, CategoriaAuto categoria, String marca, String modelo, Integer plazas,
                boolean transmisionAutomatica, BigDecimal precioBaseDia, String moneda) {
        this.destinoIata = destinoIata;
        this.categoria = categoria;
        this.marca = marca;
        this.modelo = modelo;
        this.plazas = plazas;
        this.transmisionAutomatica = transmisionAutomatica;
        this.precioBaseDia = precioBaseDia;
        this.moneda = moneda;
    }

    public void actualizar(AutoRequest r) {
        this.destinoIata = r.destinoIata().toUpperCase();
        this.categoria = r.categoria();
        this.marca = r.marca();
        this.modelo = r.modelo();
        this.plazas = r.plazas();
        this.transmisionAutomatica = r.transmisionAutomatica();
        this.precioBaseDia = r.precioBaseDia();
        this.moneda = r.moneda().toUpperCase();
    }

    public void desactivar() { this.activo = false; }

    public void marcarEnRevision() { this.enRevision = true; }

    public void aprobarRevision() { this.enRevision = false; }

    public Long getId() { return id; }
    public String getDestinoIata() { return destinoIata; }
    public CategoriaAuto getCategoria() { return categoria; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public Integer getPlazas() { return plazas; }
    public boolean isTransmisionAutomatica() { return transmisionAutomatica; }
    public BigDecimal getPrecioBaseDia() { return precioBaseDia; }
    public String getMoneda() { return moneda; }
    public boolean isActivo() { return activo; }
    public boolean isEnRevision() { return enRevision; }
}
