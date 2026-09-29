package com.travelhub.catalogo.proveedor;

import com.travelhub.common.domain.TipoProducto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Oferta normalizada que devuelve cualquier proveedor (Adapter).
 * Serializable porque se guarda en la cache de Redis.
 * Los ids que empiezan con "MOCK-" son datos simulados.
 */
public record OfertaProveedor(String id, TipoProducto tipo, String proveedor, String nombre, String destino,
                              BigDecimal precioBase, String moneda, String unidad, double ocupacion,
                              Map<String, String> detalle) implements Serializable { }
