package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalleCreacionDTO {

    private Long listaPrecioArticuloId;
    private String descripcion;
    private Double cantidad;
    private Double precioUnitario;
}
