package com.travelhub.catalogo.reservaproveedor;

import com.travelhub.catalogo.proveedor.Titular;
import com.travelhub.common.domain.TipoProducto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ReservaProveedorRequest(@NotNull TipoProducto tipo, @NotBlank String productoId,
                                      @NotNull LocalDate desde, @NotNull LocalDate hasta,
                                      @NotNull @Min(1) Integer pasajeros, @NotBlank String reservaRef,
                                      @NotNull @Valid Titular titular) { }
