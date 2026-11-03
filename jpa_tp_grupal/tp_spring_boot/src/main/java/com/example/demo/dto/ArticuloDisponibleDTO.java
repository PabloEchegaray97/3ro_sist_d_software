package com.example.demo.dto;

import java.math.BigDecimal;

public record ArticuloDisponibleDTO(
        Long listaPrecioArticuloId,
        Long articuloId,
        String codigo,
        String denominacion,
        Integer rubroCodigo,
        String rubro,
        Integer marcaCodigo,
        String marca,
        Long listaPrecioId,
        String listaPrecioCodigo,
        BigDecimal precioVenta
) {
}
