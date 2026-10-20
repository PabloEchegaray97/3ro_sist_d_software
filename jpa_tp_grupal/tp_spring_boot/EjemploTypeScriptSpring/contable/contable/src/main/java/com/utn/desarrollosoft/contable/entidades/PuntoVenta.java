package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class PuntoVenta extends AuditoriaApp {
    private int numero;
    private String descripcion;
    private String tipoEmision;
    private String domicilioComercial;
}