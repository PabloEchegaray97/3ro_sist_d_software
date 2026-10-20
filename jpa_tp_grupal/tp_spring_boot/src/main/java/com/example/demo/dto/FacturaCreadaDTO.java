package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacturaCreadaDTO {

    private Long id;
    private Long numero;
    private String estado;
    private double importeTotal;
    private int cantidadItems;
}
