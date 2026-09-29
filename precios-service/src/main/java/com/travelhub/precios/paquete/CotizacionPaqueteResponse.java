package com.travelhub.precios.paquete;

import com.travelhub.precios.cotizacion.CotizacionResponse;

import java.math.BigDecimal;
import java.util.List;

public record CotizacionPaqueteResponse(List<CotizacionResponse> items, BigDecimal subtotal,
                                        BigDecimal factorPaquete, String motivoDescuento,
                                        BigDecimal total, String moneda) { }
