package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class FacturaVentaDetalle extends EntityId {
    private FacturaVenta factura;
    private ListaPrecioArticulo listaPrecioArticulo;
    private String descripcion;
    private double cantidad;
    private double precioUnitario;
    private double porcentajeBonificacion;
    private double importeNeto;
    private double importeIva;
    private double importeSubtotal;
}