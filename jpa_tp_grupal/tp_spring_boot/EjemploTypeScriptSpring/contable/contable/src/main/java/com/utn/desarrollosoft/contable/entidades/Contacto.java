package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class Contacto extends EntityId {
    private String email;
    private String telefono;
    private String celular;
}