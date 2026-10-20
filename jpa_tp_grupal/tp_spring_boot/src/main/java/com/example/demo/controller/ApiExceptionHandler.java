package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiError> manejarSolicitudInvalida(Exception ex) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Solicitud invalida",
                mensaje(ex)
        );
        return ResponseEntity.badRequest().body(error);
    }

    private String mensaje(Exception ex) {
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "El parametro '" + mismatch.getName() + "' tiene un formato invalido";
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "El cuerpo JSON es invalido o contiene un dato con formato incorrecto";
        }
        return ex.getMessage();
    }

    public record ApiError(Instant timestamp, int status, String error, String mensaje) {
    }
}
