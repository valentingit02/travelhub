package com.travelhub.precios.cotizacion;

import com.travelhub.common.domain.TipoProducto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionRequest(
        @NotNull TipoProducto tipoProducto,
        @NotNull @DecimalMin("0.01") BigDecimal precioBase,
        @NotBlank @Size(min = 3, max = 3) String moneda,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        @NotNull @Min(1) @Max(9) Integer pasajeros,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double ocupacion) { }
