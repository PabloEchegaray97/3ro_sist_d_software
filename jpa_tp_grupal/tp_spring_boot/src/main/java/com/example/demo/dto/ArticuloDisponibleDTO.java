package com.example.demo.dto;

public record ArticuloDisponibleDTO(
        Long listaPrecioArticuloId,
        Long articuloId,
        String codigo,
        String denominacion,
        String listaPrecio,
        double precioVenta
) {
}
