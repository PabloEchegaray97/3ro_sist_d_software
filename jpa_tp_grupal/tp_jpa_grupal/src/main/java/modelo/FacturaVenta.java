package modelo;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

// TODO: Agregar @Entity y @Table
@Entity
@Table(name = "factura_venta")
public class FacturaVenta extends AuditoriaApp {

    private Long numero;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private Cliente cliente;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private TipoMoneda tipoMoneda;

    // TODO: Configurar @Column(nullable = false)
    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fechaEmision;

    // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false)
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private PuntoVenta puntoVenta;

    private double importeCobrado;
    private double importeSaldo;

    // TODO: Configurar @Column(nullable = false)
    @Column(nullable = false)
    private double importeTotal;

    private String cae;

    @Temporal(TemporalType.DATE)
    private Date caeFechaVencimiento;

    private String resultadoAfip;
    private String motivoRechazo;

    // TODO: Configurar @Column(nullable = false)
    @Column(nullable = false)
    private String estado;

    @Temporal(TemporalType.DATE)
    private Date fechaAnulacion;

    private String observaciones;

    // TODO: Configurar @OneToMany (mappedBy = "factura", cascade = CascadeType.ALL)
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL)
    private List<FacturaVentaDetalle> detalles;

    public Long getNumero() {
        return numero;
    }

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public PuntoVenta getPuntoVenta() {
        return puntoVenta;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public TipoMoneda getTipoMoneda() {
        return tipoMoneda;
    }

    public double getImporteCobrado() {
        return importeCobrado;
    }

    public double getImporteTotal() {
        return importeTotal;
    }

    public double getImporteSaldo() {
        return importeSaldo;
    }

    public String getCae() {
        return cae;
    }

    public Date getCaeFechaVencimiento() {
        return caeFechaVencimiento;
    }

    public String getResultadoAfip() {
        return resultadoAfip;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public String getEstado() {
        return estado;
    }

    public Date getFechaAnulacion() {
        return fechaAnulacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public List<FacturaVentaDetalle> getDetalles() {
        return detalles;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public void setPuntoVenta(PuntoVenta puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public void setTipoMoneda(TipoMoneda tipoMoneda) {
        this.tipoMoneda = tipoMoneda;
    }

    public void setImporteCobrado(double importeCobrado) {
        this.importeCobrado = importeCobrado;
    }

    public void setImporteSaldo(double importeSaldo) {
        this.importeSaldo = importeSaldo;
    }

    public void setImporteTotal(double importeTotal) {
        this.importeTotal = importeTotal;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public void setCaeFechaVencimiento(Date caeFechaVencimiento) {
        this.caeFechaVencimiento = caeFechaVencimiento;
    }

    public void setResultadoAfip(String resultadoAfip) {
        this.resultadoAfip = resultadoAfip;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setFechaAnulacion(Date fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public void setDetalles(List<FacturaVentaDetalle> detalles) {
        this.detalles = detalles;
    }
}