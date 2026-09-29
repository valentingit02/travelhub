package com.travelhub.precios.paquete;

import com.travelhub.precios.cotizacion.CotizacionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CotizacionPaqueteRequest(@NotEmpty List<@Valid CotizacionRequest> items) { }
