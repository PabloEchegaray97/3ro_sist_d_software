package com.example.demo.dto;

import java.util.List;

public record FacturaOpcionesDTO(
        Long siguienteNumero,
        List<OpcionDTO> puntosVenta,
        List<ClienteOpcionDTO> clientes,
        List<OpcionDTO> condicionesIva,
        List<OpcionDTO> tiposMoneda,
        OpcionDTO usuario
) {
    public record OpcionDTO(Long id, String descripcion) {
    }

    public record ClienteOpcionDTO(Long id, String descripcion, Long condicionIvaId) {
    }
}
