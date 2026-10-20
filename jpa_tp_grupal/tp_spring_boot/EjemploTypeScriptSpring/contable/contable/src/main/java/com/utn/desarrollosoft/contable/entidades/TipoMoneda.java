package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class TipoMoneda extends AuditoriaApp {
    private String codigoAfip;
    private String denominacion;
    private String simbolo;
}