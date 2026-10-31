package com.example.demo.dto;

import java.util.List;

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
            String denominacion,
            Long rubroId,
            Long marcaId,
            Long usuarioId,
            List<PrecioRequest> precios
    ) {
    }

    public record PrecioRequest(Long listaPrecioId, double precioVenta) {
    }
}
