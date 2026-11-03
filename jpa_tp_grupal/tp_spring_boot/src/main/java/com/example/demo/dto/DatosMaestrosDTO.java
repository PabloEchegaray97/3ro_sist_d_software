package com.example.demo.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
            @NotBlank String cuitCuil,
            @NotBlank String denominacion,
            String email,
            String telefono,
            String celular,
            String nombreCalle,
            String numeroCalle,
            @NotNull Long condicionIvaId,
            Long listaPrecioId,
            @NotNull Long usuarioId
    ) {
    }

    public record ClienteListaPrecioRequest(Long listaPrecioId, Long usuarioId) {
    }

    public record RubroRequest(
            @NotBlank String denominacion,
            @NotNull Long usuarioId
    ) {
    }

    public record MarcaRequest(
            @NotBlank String denominacion,
            @NotNull Long usuarioId
    ) {
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
            @NotNull Long listaPrecioId,
            @NotNull Long articuloId,
            @NotNull @DecimalMin("0.00") BigDecimal precioVenta,
            @NotNull Long usuarioId
    ) {
    }
}
