package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

// CONSIGNA - PARTE 2.1 y 2.2: DTO ejecutivo con unicamente los siete campos
// solicitados y definido de forma concisa mediante Lombok.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacturaReporteDTO {

    // CONSIGNA - PARTE 2.1: campos exactos pedidos para el reporte.
    private Long numeroFactura;
    private Date fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private double importeTotal;
    private long cantidadItems;
}
