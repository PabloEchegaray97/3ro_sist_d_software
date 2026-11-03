package com.example.demo.controller;

import com.example.demo.exception.ConflictoNegocioException;
import com.example.demo.exception.PrecisionMonetariaException;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.service.ArcaHomologacionService;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), "recurso-no-encontrado");
    }

    @ExceptionHandler({ConflictoNegocioException.class, OptimisticLockException.class,
            PessimisticLockException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ProblemDetail> manejarConflicto(Exception ex) {
        String detalle = ex instanceof ConflictoNegocioException
                ? ex.getMessage()
                : "El recurso fue modificado por otra operación o viola una restricción única";
        return problema(HttpStatus.CONFLICT, "Conflicto de negocio", detalle, "conflicto");
    }

    @ExceptionHandler(PrecisionMonetariaException.class)
    public ResponseEntity<ProblemDetail> manejarPrecision(PrecisionMonetariaException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Precisión monetaria inválida",
                ex.getMessage(), "precision-monetaria");
    }

    @ExceptionHandler(ArcaHomologacionService.ArcaException.class)
    public ResponseEntity<ProblemDetail> manejarArca(ArcaHomologacionService.ArcaException ex) {
        return problema(HttpStatus.BAD_GATEWAY, "Error de comunicación con ARCA",
                ex.getMessage(), "arca");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail detalle = crear(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "Uno o más campos no superaron la validación", "validacion");
        detalle.setProperty("fieldErrors", errores);
        return ResponseEntity.badRequest().body(detalle);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> manejarSolicitudInvalida(Exception ex) {
        String mensaje;
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            mensaje = "El parámetro '" + mismatch.getName() + "' tiene un formato inválido";
        } else if (ex instanceof HttpMessageNotReadableException) {
            mensaje = "El cuerpo JSON es inválido o contiene un dato con formato incorrecto";
        } else {
            mensaje = ex.getMessage();
        }
        return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", mensaje, "solicitud-invalida");
    }

    private ResponseEntity<ProblemDetail> problema(HttpStatus estado, String titulo,
                                                    String detalle, String tipo) {
        return ResponseEntity.status(estado).body(crear(estado, titulo, detalle, tipo));
    }

    private ProblemDetail crear(HttpStatus estado, String titulo, String detalle, String tipo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle == null ? titulo : detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://tp-facturacion.local/problems/" + tipo));
        return problema;
    }
}
