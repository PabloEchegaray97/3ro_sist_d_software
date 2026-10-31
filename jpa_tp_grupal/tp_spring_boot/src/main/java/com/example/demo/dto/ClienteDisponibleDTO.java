package com.example.demo.dto;

import java.util.Date;

public record ClienteDisponibleDTO(
        Long id,
        String cuitCuil,
        String denominacion,
        Long condicionIvaId,
        String condicionIva,
        String email,
        String telefono,
        String celular,
        String nombreCalle,
        String numeroCalle,
        Long listaPrecioId,
        String listaPrecioCodigo,
        String listaPrecio,
        Date fechaAlta,
        Date ultimaCompra
) {
}
