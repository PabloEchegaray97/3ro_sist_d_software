package com.utn.desarrollosoft.contable.entidades.dto;


import java.io.Serializable;


public class FacturaVentaDetalleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String productoDescripcion;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    public FacturaVentaDetalleDTO() {
    }

    public FacturaVentaDetalleDTO(Long id, String productoDescripcion, Integer cantidad, Double precioUnitario) {
        this.id = id;
        this.productoDescripcion = productoDescripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = calcularSubtotal();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductoDescripcion() {
        return productoDescripcion;
    }

    public void setProductoDescripcion(String productoDescripcion) {
        this.productoDescripcion = productoDescripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
        this.subtotal = calcularSubtotal();
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
        this.subtotal = calcularSubtotal();
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    private Double calcularSubtotal() {
        if (cantidad == null || precioUnitario == null) {
            return 0.0;
        }
        return cantidad * precioUnitario;
    }
}
