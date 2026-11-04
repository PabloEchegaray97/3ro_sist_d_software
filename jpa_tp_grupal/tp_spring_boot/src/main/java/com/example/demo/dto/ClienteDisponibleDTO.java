package com.example.demo.dto;

import com.example.demo.model.TipoDocumento;
import java.util.Date;

public record ClienteDisponibleDTO(
        Long id,
        TipoDocumento tipoDocumento,
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
        Date fechaBaja,
        Date ultimaCompra
) {
}
