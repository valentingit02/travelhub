package com.travelhub.catalogo.oferta;

import com.travelhub.common.domain.TipoProducto;

import java.math.BigDecimal;

/** Lo que el catalogo garantiza sobre una oferta al momento de reservar (precio en USD). */
public record OfertaVerificada(String id, TipoProducto tipo, String proveedor, String nombre, String destino,
                               BigDecimal precio, String moneda, double ocupacion) { }
