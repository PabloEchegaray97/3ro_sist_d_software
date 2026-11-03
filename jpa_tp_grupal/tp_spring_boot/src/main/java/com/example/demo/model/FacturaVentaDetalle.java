package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "factura_venta_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacturaVentaDetalle extends EntityId {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private FacturaVenta factura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_precio_articulo_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private ListaPrecioArticulo listaPrecioArticulo;

    private String descripcion;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal cantidad;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @Column(precision = 7, scale = 4, nullable = false)
    private BigDecimal porcentajeBonificacion;
    @Column(precision = 7, scale = 4, nullable = false)
    private BigDecimal porcentajeIva;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal importeNeto;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal importeIva;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal importeSubtotal;
}
