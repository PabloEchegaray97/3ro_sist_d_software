package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

public record ArcaEmisionDTO(
        boolean emitida,
        Long facturaId,
        Long numero,
        String estado,
        String cae,
        @JsonFormat(pattern = "yyyy-MM-dd") Date caeFechaVencimiento,
        String mensaje,
        List<MensajeDTO> mensajes
) {
    public record MensajeDTO(String tipo, String codigo, String descripcion) { }
}
