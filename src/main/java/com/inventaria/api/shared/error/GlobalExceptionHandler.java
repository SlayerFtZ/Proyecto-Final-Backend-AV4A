package com.inventaria.api.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDACION", "Hay campos inválidos", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ApiErrorDetail> details = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return new ApiErrorDetail(field, cv.getMessage());
                })
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDACION", "Parámetros inválidos", details, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("DataIntegrityViolation on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "REGISTRO_EN_USO",
                "No se puede completar la operación porque el registro está en uso o viola una restricción de integridad",
                null, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        return build(HttpStatus.CONTENT_TOO_LARGE, "ARCHIVO_INVALIDO",
                "El archivo supera el tamaño máximo permitido (5 MB)", null, request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(
            DomainException ex, HttpServletRequest request) {

        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado. Por favor contacte al administrador.", null, request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String code, String message,
            List<ApiErrorDetail> details, HttpServletRequest request) {

        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                code,
                message,
                details,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
