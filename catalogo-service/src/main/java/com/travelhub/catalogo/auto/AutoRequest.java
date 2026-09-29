package com.travelhub.catalogo.auto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AutoRequest(
        @NotBlank @Size(min = 3, max = 3) String destinoIata,
        @NotNull CategoriaAuto categoria,
        @NotBlank String marca,
        @NotBlank String modelo,
        @NotNull @Min(2) @Max(9) Integer plazas,
        boolean transmisionAutomatica,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precioBaseDia,
        @NotBlank @Size(min = 3, max = 3) String moneda) { }
