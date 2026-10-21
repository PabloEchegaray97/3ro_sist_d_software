package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaVistaDTO {

    private Long id;
    private Long numero;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date fechaEmision;
    private Long puntoVentaId;
    private String puntoVentaDescripcion;
    private Long clienteId;
    private String clienteDenominacion;
    private Long condicionIvaId;
    private String condicionIva;
    private Long tipoMonedaId;
    private String tipoMoneda;
    private Long usuarioId;
    private String usuario;
    private String estado;
    private double importeCobrado;
    private double importeSaldo;
    private double importeTotal;
    private String observaciones;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaAnulacion;
    private List<FacturaDetalleVistaDTO> detalles;
}
