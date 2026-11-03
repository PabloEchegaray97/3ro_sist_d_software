package com.example.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(
        name = "factura_venta",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_factura_fiscal",
                columnNames = {"punto_venta_id", "tipo_comprobante", "numero"}
        )
)
// CONSIGNA - PARTE 1.2 y 1.3: no se usa @Data; las entidades usan
// @Getter/@Setter y constructores de Lombok para trabajar de forma segura con JPA.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// CONSIGNA - PARTE 1.3: FacturaVenta hereda de AuditoriaApp.
@EqualsAndHashCode(callSuper = true)
public class FacturaVenta extends AuditoriaApp {

    private Long numero;

    @Column(name = "tipo_comprobante", nullable = false, columnDefinition = "int default 11")
    @Builder.Default
    private int tipoComprobante = 11;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_venta_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private PuntoVenta puntoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @EqualsAndHashCode.Exclude
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id")
    @EqualsAndHashCode.Exclude
    private ListaPrecio listaPrecio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condicion_iva_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private CondicionIva condicionIva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_moneda_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private TipoMoneda tipoMoneda;

    @Column(precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal importeCobrado = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal importeSaldo = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal importeTotal = BigDecimal.ZERO;

    private String cae;

    @Temporal(TemporalType.DATE)
    private Date caeFechaVencimiento;

    private String resultadoAfip;
    private String motivoRechazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoFactura estado = EstadoFactura.SIN_EMITIR;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    @Temporal(TemporalType.DATE)
    private Date fechaAnulacion;

    private String observaciones;

    // CONSIGNA ESCRITURA - 2.b y 2.d: cascade ALL persiste y elimina los
    // detalles junto con la factura; orphanRemoval elimina items reemplazados.
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    // CONSIGNA - PARTE 1.3: la coleccion bidireccional queda fuera de
    // equals/hashCode y evita recursion infinita con FacturaVentaDetalle.
    @EqualsAndHashCode.Exclude
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(FacturaVentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFactura(this);
    }
}
