package com.example.demo.controller;

import com.example.demo.dto.DatosMaestrosDTO;
import com.example.demo.dto.EntidadCreadaDTO;
import com.example.demo.dto.ArticuloDisponibleDTO;
import com.example.demo.dto.ArticuloAltaDTO;
import com.example.demo.dto.ClienteDisponibleDTO;
import com.example.demo.dto.PaginaDTO;
import com.example.demo.dto.FacturaOpcionesDTO;
import com.example.demo.service.DatosMaestrosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datos-maestros")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class DatosMaestrosRestController {

    private final DatosMaestrosService service;

    @GetMapping("/precios-articulo")
    public PaginaDTO<ArticuloDisponibleDTO> buscarArticulosDisponibles(
            @RequestParam(defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "8") int tamanio,
            @RequestParam(required = false) Long listaPrecioId
    ) {
        return service.buscarArticulosDisponibles(busqueda, pagina, tamanio, listaPrecioId);
    }

    @GetMapping("/clientes-disponibles")
    public PaginaDTO<ClienteDisponibleDTO> buscarClientesDisponibles(
            @RequestParam(defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "8") int tamanio,
            @RequestParam(defaultValue = "ULTIMOS_AGREGADOS") String orden
    ) {
        return service.buscarClientesDisponibles(busqueda, pagina, tamanio, orden);
    }

    @GetMapping("/opciones-factura")
    public FacturaOpcionesDTO buscarOpcionesFactura() {
        return service.buscarOpcionesFactura();
    }

    @GetMapping("/articulos/opciones")
    public ArticuloAltaDTO.Opciones buscarOpcionesAltaArticulo() {
        return service.buscarOpcionesAltaArticulo();
    }

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

    @PutMapping("/clientes/{clienteId}/lista-precio")
    public EntidadCreadaDTO actualizarListaPrecioCliente(
            @PathVariable Long clienteId,
            @RequestBody DatosMaestrosDTO.ClienteListaPrecioRequest request
    ) {
        return service.actualizarListaPrecioCliente(clienteId, request);
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

    @PostMapping("/articulos-con-precios")
    public ResponseEntity<EntidadCreadaDTO> crearArticuloConPrecios(
            @RequestBody ArticuloAltaDTO.CrearRequest request
    ) {
        return creado(service.crearArticuloConPrecios(request));
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
