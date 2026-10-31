package com.example.demo.dto;

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
        String listaPrecio,
        double precioVenta
) {
}
