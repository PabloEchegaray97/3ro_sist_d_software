package com.example.demo.dto;

import java.time.Instant;

public record ArcaAutenticacionDTO(
        boolean autenticada,
        String ambiente,
        String servicio,
        Instant vencimiento,
        String mensaje
) {
}
