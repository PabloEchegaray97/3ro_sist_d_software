package com.utn.desarrollosoft.contable.entidades.dto;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class FacturaVentaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long numeroFactura;
    private Date fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private Double importeTotal;
    private List<FacturaVentaDetalleDTO> detalles;

    public FacturaVentaDTO() {
        this.detalles = new ArrayList<>();
    }

    public FacturaVentaDTO(Long numeroFactura, Date fechaEmision,
                           String clienteDenominacion, String condicionIva,
                           String puntoVentaDescripcion, Double importeTotal,
                           List<FacturaVentaDetalleDTO> detalles) {
        this.numeroFactura = numeroFactura;
        this.fechaEmision = fechaEmision;
        this.clienteDenominacion = clienteDenominacion;
        this.condicionIva = condicionIva;
        this.puntoVentaDescripcion = puntoVentaDescripcion;
        this.detalles = detalles != null ? detalles : new ArrayList<>();
        this.importeTotal = importeTotal;
    }

    public Long getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(Long numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getClienteDenominacion() {
        return clienteDenominacion;
    }

    public void setClienteDenominacion(String clienteDenominacion) {
        this.clienteDenominacion = clienteDenominacion;
    }

    public String getCondicionIva() {
        return condicionIva;
    }

    public void setCondicionIva(String condicionIva) {
        this.condicionIva = condicionIva;
    }

    public String getPuntoVentaDescripcion() {
        return puntoVentaDescripcion;
    }

    public void setPuntoVentaDescripcion(String puntoVentaDescripcion) {
        this.puntoVentaDescripcion = puntoVentaDescripcion;
    }

    public Double getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(Double importeTotal) {
        this.importeTotal = importeTotal;
    }

    public List<FacturaVentaDetalleDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<FacturaVentaDetalleDTO> detalles) {
        this.detalles = detalles != null ? detalles : new ArrayList<>();
    }

    public Integer getCantidadItems() {
        return detalles != null ? detalles.size() : 0;
    }
}
