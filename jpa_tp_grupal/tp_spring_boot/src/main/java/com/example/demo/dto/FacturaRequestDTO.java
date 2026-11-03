package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// CONSIGNA ESCRITURA - 1.a y 1.b: Request DTO para POST y PUT.
// Las relaciones se reciben por ID; nunca se exponen entidades JPA en el JSON.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRequestDTO {

    @NotNull
    private Long puntoVentaId;
    private Long clienteId;
    @NotNull
    private Long listaPrecioId;
    @NotNull
    private Long condicionIvaId;
    @NotNull
    private Long tipoMonedaId;
    @NotNull
    private Long usuarioId;
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal importeCobrado;
    @Size(max = 1000)
    private String observaciones;
    @NotEmpty
    @Valid
    private List<FacturaDetalleRequestDTO> detalles;
}
