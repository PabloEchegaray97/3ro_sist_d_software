package com.example.demo.exception;

// CONSIGNA ESCRITURA - 2.b, 2.c y 3.1: permite traducir recursos inexistentes
// al codigo HTTP 404 desde el manejador global de errores.
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
