package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

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
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal porcentajeBonificacion;
    private BigDecimal porcentajeIva;
    private BigDecimal importeNeto;
    private BigDecimal importeIva;
    private BigDecimal importeSubtotal;
}
