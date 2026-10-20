package com.example.demo.controller;

import com.example.demo.dto.DatosMaestrosDTO;
import com.example.demo.dto.EntidadCreadaDTO;
import com.example.demo.service.DatosMaestrosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datos-maestros")
@RequiredArgsConstructor
public class DatosMaestrosRestController {

    private final DatosMaestrosService service;

    @PostMapping("/usuarios")
    public ResponseEntity<EntidadCreadaDTO> crearUsuario(
            @RequestBody DatosMaestrosDTO.UsuarioRequest request
    ) {
        return creado(service.crearUsuario(request));
    }

    @PostMapping("/condiciones-iva")
    public ResponseEntity<EntidadCreadaDTO> crearCondicionIva(
            @RequestBody DatosMaestrosDTO.CondicionIvaRequest request
    ) {
        return creado(service.crearCondicionIva(request));
    }

    @PostMapping("/tipos-moneda")
    public ResponseEntity<EntidadCreadaDTO> crearTipoMoneda(
            @RequestBody DatosMaestrosDTO.TipoMonedaRequest request
    ) {
        return creado(service.crearTipoMoneda(request));
    }

    @PostMapping("/puntos-venta")
    public ResponseEntity<EntidadCreadaDTO> crearPuntoVenta(
            @RequestBody DatosMaestrosDTO.PuntoVentaRequest request
    ) {
        return creado(service.crearPuntoVenta(request));
    }

    @PostMapping("/clientes")
    public ResponseEntity<EntidadCreadaDTO> crearCliente(
            @RequestBody DatosMaestrosDTO.ClienteRequest request
    ) {
        return creado(service.crearCliente(request));
    }

    @PostMapping("/rubros")
    public ResponseEntity<EntidadCreadaDTO> crearRubro(
            @RequestBody DatosMaestrosDTO.RubroRequest request
    ) {
        return creado(service.crearRubro(request));
    }

    @PostMapping("/marcas")
    public ResponseEntity<EntidadCreadaDTO> crearMarca(
            @RequestBody DatosMaestrosDTO.MarcaRequest request
    ) {
        return creado(service.crearMarca(request));
    }

    @PostMapping("/listas-precio")
    public ResponseEntity<EntidadCreadaDTO> crearListaPrecio(
            @RequestBody DatosMaestrosDTO.ListaPrecioRequest request
    ) {
        return creado(service.crearListaPrecio(request));
    }

    @PostMapping("/articulos")
    public ResponseEntity<EntidadCreadaDTO> crearArticulo(
            @RequestBody DatosMaestrosDTO.ArticuloRequest request
    ) {
        return creado(service.crearArticulo(request));
    }

    @PostMapping("/precios-articulo")
    public ResponseEntity<EntidadCreadaDTO> crearPrecioArticulo(
            @RequestBody DatosMaestrosDTO.ListaPrecioArticuloRequest request
    ) {
        return creado(service.crearPrecio(request));
    }

    private ResponseEntity<EntidadCreadaDTO> creado(EntidadCreadaDTO entidad) {
        return ResponseEntity.status(201).body(entidad);
    }
}
