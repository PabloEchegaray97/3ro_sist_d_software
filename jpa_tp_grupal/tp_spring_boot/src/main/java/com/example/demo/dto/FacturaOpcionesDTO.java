package com.example.demo.dto;

import java.util.List;

public record FacturaOpcionesDTO(
        Long siguienteNumero,
        List<OpcionDTO> puntosVenta,
        List<ClienteOpcionDTO> clientes,
        List<OpcionDTO> condicionesIva,
        List<OpcionDTO> tiposMoneda,
        List<OpcionDTO> listasPrecio,
        OpcionDTO listaPrecioConsumidorFinal,
        OpcionDTO usuario
) {
    public record OpcionDTO(Long id, String descripcion) {
    }

    public record ClienteOpcionDTO(
            Long id,
            String descripcion,
            Long condicionIvaId,
            Long listaPrecioId,
            String listaPrecio
    ) {
    }
}
