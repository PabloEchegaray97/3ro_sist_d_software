package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class CondicionIva extends AuditoriaApp {
    private int codigoAfip;
    private String denominacion;
}