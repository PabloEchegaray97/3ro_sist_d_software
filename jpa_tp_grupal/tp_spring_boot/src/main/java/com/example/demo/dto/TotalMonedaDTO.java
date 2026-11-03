package com.example.demo.dto;

import java.math.BigDecimal;

public record TotalMonedaDTO(
        String codigoMoneda,
        String moneda,
        BigDecimal total
) {
}
