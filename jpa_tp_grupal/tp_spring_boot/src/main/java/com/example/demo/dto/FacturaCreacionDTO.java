package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaCreacionDTO {

    private Long numero;
    private LocalDate fechaEmision;
    private Long puntoVentaId;
    private Long clienteId;
    private Long condicionIvaId;
    private Long tipoMonedaId;
    private Long usuarioId;
    private String estado;
    private Double importeCobrado;
    private String observaciones;
    private List<FacturaDetalleCreacionDTO> detalles = new ArrayList<>();
}
