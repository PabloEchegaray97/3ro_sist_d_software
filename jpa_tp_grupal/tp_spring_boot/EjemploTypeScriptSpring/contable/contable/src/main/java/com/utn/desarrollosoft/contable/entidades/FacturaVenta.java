package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class FacturaVenta extends AuditoriaApp {
    private Long numero;
    private Date fechaEmision;
    private PuntoVenta puntoVenta;
    private Cliente cliente;
    private CondicionIva condicionIva;
    private TipoMoneda tipoMoneda;
    private double importeCobrado;
    private double importeSaldo;
    private double importeTotal;
    private String cae;
    private Date caeFechaVencimiento;
    private String resultadoAfip;
    private String motivoRechazo;
    private String estado;
    private Date fechaAnulacion;
    private String observaciones;
    private List<FacturaVentaDetalle> detalles;
}