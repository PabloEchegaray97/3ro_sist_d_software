package com.example.demo.dto;

public final class DatosMaestrosDTO {

    private DatosMaestrosDTO() {
    }

    public record UsuarioRequest(String usuario, String clave, String nombre, String apellido) {
    }

    public record CondicionIvaRequest(int codigoAfip, String denominacion, Long usuarioId) {
    }

    public record TipoMonedaRequest(
            String codigoAfip,
            String denominacion,
            String simbolo,
            Long usuarioId
    ) {
    }

    public record PuntoVentaRequest(
            int numero,
            String descripcion,
            String tipoEmision,
            String domicilioComercial,
            Long usuarioId
    ) {
    }

    public record ClienteRequest(
            String cuitCuil,
            String denominacion,
            String email,
            String telefono,
            String celular,
            String nombreCalle,
            String numeroCalle,
            Long usuarioId
    ) {
    }

    public record RubroRequest(Integer codigo, String denominacion, Long usuarioId) {
    }

    public record MarcaRequest(Integer codigo, String denominacion, Long usuarioId) {
    }

    public record ListaPrecioRequest(String codigo, String denominacion, Long usuarioId) {
    }

    public record ArticuloRequest(
            String codigo,
            String denominacion,
            Long rubroId,
            Long marcaId,
            Long usuarioId
    ) {
    }

    public record ListaPrecioArticuloRequest(
            Long listaPrecioId,
            Long articuloId,
            double precioVenta,
            Long usuarioId
    ) {
    }
}
