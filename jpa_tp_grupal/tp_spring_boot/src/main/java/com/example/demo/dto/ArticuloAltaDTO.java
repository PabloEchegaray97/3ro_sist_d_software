package com.example.demo.dto;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class ArticuloAltaDTO {

    private ArticuloAltaDTO() {
    }

    public record Opciones(
            List<Opcion> rubros,
            List<Opcion> marcas,
            List<ListaPrecioOpcion> listasPrecio,
            Opcion usuario
    ) {
    }

    public record Opcion(Long id, String descripcion) {
    }

    public record ListaPrecioOpcion(Long id, String codigo, String denominacion) {
    }

    public record CrearRequest(
            String codigo,
            @NotBlank String denominacion,
            @NotNull Long rubroId,
            Long marcaId,
            @NotNull Long usuarioId,
            @NotEmpty @Valid List<PrecioRequest> precios
    ) {
    }

    public record PrecioRequest(
            @NotNull Long listaPrecioId,
            @NotNull @DecimalMin("0.00") BigDecimal precioVenta
    ) {
    }
}
