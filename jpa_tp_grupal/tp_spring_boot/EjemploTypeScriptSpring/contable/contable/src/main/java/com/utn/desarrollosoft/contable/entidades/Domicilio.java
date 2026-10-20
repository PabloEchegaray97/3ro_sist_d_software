package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class Domicilio extends EntityId {
    private String nombreCalle;
    private String numeroCalle;
}