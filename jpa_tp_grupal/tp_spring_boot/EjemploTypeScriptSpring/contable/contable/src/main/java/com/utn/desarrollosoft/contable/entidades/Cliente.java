package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class Cliente extends AuditoriaApp {
    private String cuitCuil;
    private String denominacion;
    private Contacto contacto;
    private Domicilio domicilio;
}