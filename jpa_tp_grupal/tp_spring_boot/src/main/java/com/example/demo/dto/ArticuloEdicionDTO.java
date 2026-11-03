package com.example.demo.dto;

import java.util.List;
import java.math.BigDecimal;

public record ArticuloEdicionDTO(
        Long id,
        String codigo,
        String denominacion,
        Long rubroId,
        Long marcaId,
        List<PrecioDTO> precios
) {
    public record PrecioDTO(Long listaPrecioId, BigDecimal precioVenta) { }
}
