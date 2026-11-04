package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.math.BigDecimal;
import com.example.demo.model.EstadoFactura;

// CONSIGNA - PARTE 2.1 y 2.2: DTO ejecutivo definido mediante Lombok.
// CONSIGNA ESCRITURA - 4.a y 5.b: id identifica la fila para PUT y DELETE.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacturaReporteDTO {

    private Long id;
    private Long numeroFactura;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date fechaEmision;
    // Fecha operativa interna. Reutiliza fechaAlta de AuditoriaApp y no
    // representa la fecha fiscal que ARCA asigna al emitir.
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date fechaCreacionInterna;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private BigDecimal importeTotal;
    private long cantidadItems;
    private EstadoFactura estado;
}
