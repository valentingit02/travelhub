package com.travelhub.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/** Manejo de errores centralizado: todas las respuestas de error tienen el mismo formato. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> externo(ExternalServiceException ex) {
        log.warn("Fallo de servicio externo: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Datos invalidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo del request no es un JSON valido", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rutaInexistente(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Ruta inexistente", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generico(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String mensaje, List<String> detalles) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), mensaje,
                MDC.get(CorrelationIdFilter.MDC_KEY), Instant.now(), detalles);
        return ResponseEntity.status(status).body(body);
    }
}
