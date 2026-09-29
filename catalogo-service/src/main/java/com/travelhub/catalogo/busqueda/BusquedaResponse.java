package com.travelhub.catalogo.busqueda;

import java.util.List;

public record BusquedaResponse(List<ProductoResponse> vuelos, List<ProductoResponse> hoteles,
                               List<ProductoResponse> excursiones, List<ProductoResponse> autos) { }
