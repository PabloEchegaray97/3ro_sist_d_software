package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// CONSIGNA - PARTE 1.3: la clase hereda de EntityId e incluye callSuper = true.
@EqualsAndHashCode(callSuper = true)
public abstract class AuditoriaApp extends EntityId {

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date fechaAlta;

    @Temporal(TemporalType.TIMESTAMP)
    protected Date fechaBaja;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    protected Date fechaModificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_carga_id", nullable = false)
    // CONSIGNA - PARTE 1.3: se excluyen relaciones JPA del equals/hashCode
    // para evitar recorridos recursivos entre entidades relacionadas.
    @EqualsAndHashCode.Exclude
    protected Usuario usuarioCarga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_baja_id")
    @EqualsAndHashCode.Exclude
    protected Usuario usuarioBaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_modificacion_id", nullable = false)
    @EqualsAndHashCode.Exclude
    protected Usuario usuarioModificacion;
}
