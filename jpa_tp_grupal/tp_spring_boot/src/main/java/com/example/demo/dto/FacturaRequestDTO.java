package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// CONSIGNA ESCRITURA - 1.a y 1.b: Request DTO para POST y PUT.
// Las relaciones se reciben por ID; nunca se exponen entidades JPA en el JSON.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRequestDTO {

    private Long numero;
    private LocalDateTime fechaEmision;
    private Long puntoVentaId;
    private Long clienteId;
    private Long condicionIvaId;
    private Long tipoMonedaId;
    private Long usuarioId;
    private String estado;
    private Double importeCobrado;
    private String observaciones;
    private List<FacturaDetalleRequestDTO> detalles;
}
