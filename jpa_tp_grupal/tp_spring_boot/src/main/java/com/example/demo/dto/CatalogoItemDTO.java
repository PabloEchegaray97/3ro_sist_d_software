package com.example.demo.dto;

import java.util.Date;

public record CatalogoItemDTO(
        Long id,
        Integer codigo,
        String denominacion,
        Date fechaAlta,
        Date fechaBaja
) {
}
