package com.travelhub.catalogo.busqueda;

import com.travelhub.catalogo.proveedor.OfertaProveedor;
import com.travelhub.common.domain.ProductoTuristico;
import com.travelhub.common.domain.TipoProducto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductoResponse(String id, TipoProducto tipo, String proveedor, String nombre, String descripcion,
                               String destino, BigDecimal precioBase, String moneda, String unidad,
                               double ocupacion, Map<String, String> detalle) {

    public static ProductoResponse from(ProductoTuristico p, OfertaProveedor o) {
        return new ProductoResponse(p.getId(), p.getTipo(), o.proveedor(), o.nombre(), p.descripcion(),
                p.getDestino(), p.getPrecioBase().monto(), p.getPrecioBase().moneda(), o.unidad(),
                o.ocupacion(), o.detalle());
    }
}
