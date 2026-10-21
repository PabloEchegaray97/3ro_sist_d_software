package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalleVistaDTO {

    private Long id;
    private Long articuloId;
    private String articuloCodigo;
    private String articuloDenominacion;
    private Long listaPrecioArticuloId;
    private String listaPrecio;
    private String descripcion;
    private double cantidad;
    private double precioUnitario;
    private double porcentajeBonificacion;
    private double porcentajeIva;
    private double importeNeto;
    private double importeIva;
    private double importeSubtotal;
}
