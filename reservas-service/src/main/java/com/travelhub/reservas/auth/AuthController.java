package com.travelhub.reservas.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Registro e ingreso con email y contrasena. Devuelve un JWT para el header Authorization")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea la cuenta y abre la sesion")
    public AuthDtos.SesionResponse registro(@Valid @RequestBody AuthDtos.RegistroRequest r) {
        return service.registrar(r);
    }

    @PostMapping("/login")
    @Operation(summary = "Ingresa con email y contrasena (401 si no coinciden; 409 si la cuenta todavia no tiene contrasena)")
    public AuthDtos.SesionResponse login(@Valid @RequestBody AuthDtos.LoginRequest r) {
        return service.ingresar(r);
    }

    @PostMapping("/activar")
    @Operation(summary = "Crea la contrasena de una cuenta anterior al login, confirmando el documento")
    public AuthDtos.SesionResponse activar(@Valid @RequestBody AuthDtos.ActivarRequest r) {
        return service.activar(r);
    }
}
