package com.travelhub.catalogo.reservaproveedor;

import com.travelhub.common.domain.TipoProducto;

public record ReservaProveedorResponse(TipoProducto tipo, String productoId, String refExterna) { }
