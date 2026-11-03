package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// CONSIGNA ESCRITURA - 1.a y 1.b: cada renglon referencia Articulo y
// ListaPrecioArticulo mediante IDs y contiene los datos necesarios para calcular importes.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalleRequestDTO {

    @NotNull
    private Long articuloId;
    @NotNull
    private Long listaPrecioArticuloId;
    @Size(max = 255)
    private String descripcion;
    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal cantidad;
    @DecimalMin(value = "0.00")
    private BigDecimal precioUnitario;
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal porcentajeBonificacion;
    @DecimalMin(value = "0.00")
    private BigDecimal porcentajeIva;
}
