package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// CONSIGNA ESCRITURA - 1.a y 1.b: cada renglon referencia Articulo y
// ListaPrecioArticulo mediante IDs y contiene los datos necesarios para calcular importes.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalleRequestDTO {

    private Long articuloId;
    private Long listaPrecioArticuloId;
    private String descripcion;
    private Double cantidad;
    private Double precioUnitario;
    private Double porcentajeBonificacion;
    private Double porcentajeIva;
}
