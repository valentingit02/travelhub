package com.travelhub.catalogo.proveedor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record Titular(@NotBlank String nombre, @NotBlank String apellido, @NotBlank @Email String email,
                      @NotNull LocalDate fechaNacimiento) { }
