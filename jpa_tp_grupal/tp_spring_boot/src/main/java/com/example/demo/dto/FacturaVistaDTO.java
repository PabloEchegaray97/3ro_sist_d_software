package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

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
    private int puntoVentaNumero;
    private String puntoVentaDescripcion;
    private String puntoVentaDomicilio;
    private int tipoComprobante;
    private Long clienteId;
    private String clienteTipoDocumento;
    private String clienteDenominacion;
    private String clienteCuitCuil;
    private String clienteTelefono;
    private String clienteEmail;
    private String clienteDomicilio;
    private Long listaPrecioId;
    private String listaPrecioCodigo;
    private String listaPrecio;
    private Long condicionIvaId;
    private String condicionIva;
    private Long tipoMonedaId;
    private String tipoMonedaCodigo;
    private String tipoMoneda;
    private Long usuarioId;
    private String usuario;
    private String estado;
    private BigDecimal importeCobrado;
    private BigDecimal importeSaldo;
    private BigDecimal importeTotal;
    private String observaciones;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fechaAnulacion;
    private String cae;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date caeFechaVencimiento;
    private String resultadoAfip;
    private String motivoRechazo;
    private List<FacturaDetalleVistaDTO> detalles;
}
