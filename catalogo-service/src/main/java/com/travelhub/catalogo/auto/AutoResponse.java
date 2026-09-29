package com.travelhub.catalogo.auto;

import java.math.BigDecimal;

public record AutoResponse(Long id, String destinoIata, CategoriaAuto categoria, String marca, String modelo,
                           Integer plazas, boolean transmisionAutomatica, BigDecimal precioBaseDia,
                           String moneda, boolean activo, boolean enRevision) {

    public static AutoResponse from(Auto a) {
        return new AutoResponse(a.getId(), a.getDestinoIata(), a.getCategoria(), a.getMarca(), a.getModelo(),
                a.getPlazas(), a.isTransmisionAutomatica(), a.getPrecioBaseDia(), a.getMoneda(), a.isActivo(),
                a.isEnRevision());
    }
}
