package com.example.demo.dto;

import java.util.List;

public record SistemaInfoDTO(
        List<PuntoVentaInfo> puntosVenta,
        List<CondicionIvaInfo> condicionesIva,
        List<MonedaInfo> monedas,
        List<ListaPrecioInfo> listasPrecio,
        UsuarioInfo usuario
) {
    public record PuntoVentaInfo(
            Long id,
            int numero,
            String descripcion,
            String tipoEmision,
            String domicilioComercial
    ) {
    }

    public record CondicionIvaInfo(Long id, int codigoAfip, String denominacion) {
    }

    public record MonedaInfo(Long id, String codigoAfip, String denominacion, String simbolo) {
    }

    public record ListaPrecioInfo(Long id, String codigo, String denominacion) {
    }

    public record UsuarioInfo(Long id, String usuario, String nombreCompleto) {
    }
}
