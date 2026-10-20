package com.utn.desarrollosoft.contable.entidades;

import lombok.Data;

@Data
public class ListaPrecioArticulo extends AuditoriaApp {
    private ListaPrecio listaPrecio;
    private double precioVenta;
    private Articulo articulo;
}