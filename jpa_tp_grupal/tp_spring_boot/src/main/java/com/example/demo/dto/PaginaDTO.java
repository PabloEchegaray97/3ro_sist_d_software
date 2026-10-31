package com.example.demo.dto;

import java.util.List;

public record PaginaDTO<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas
) {
}
