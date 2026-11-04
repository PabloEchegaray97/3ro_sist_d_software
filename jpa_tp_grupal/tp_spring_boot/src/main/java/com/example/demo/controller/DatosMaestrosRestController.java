package com.example.demo.controller;

import com.example.demo.dto.DatosMaestrosDTO;
import com.example.demo.dto.EntidadCreadaDTO;
import com.example.demo.dto.ArticuloDisponibleDTO;
import com.example.demo.dto.ArticuloAltaDTO;
import com.example.demo.dto.ArticuloEdicionDTO;
import com.example.demo.dto.ClienteDisponibleDTO;
import com.example.demo.dto.CatalogoItemDTO;
import com.example.demo.dto.PaginaDTO;
import com.example.demo.dto.FacturaOpcionesDTO;
import com.example.demo.dto.SistemaInfoDTO;
import com.example.demo.service.DatosMaestrosService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
            @RequestParam(required = false) Long listaPrecioId,
            @RequestParam(defaultValue = "ACTIVOS") String estado
    ) {
        return service.buscarArticulosDisponibles(busqueda, pagina, tamanio, listaPrecioId, estado);
    }

    @GetMapping("/clientes-disponibles")
    public PaginaDTO<ClienteDisponibleDTO> buscarClientesDisponibles(
            @RequestParam(defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "8") int tamanio,
            @RequestParam(defaultValue = "ULTIMOS_AGREGADOS") String orden,
            @RequestParam(defaultValue = "ACTIVOS") String estado
    ) {
        return service.buscarClientesDisponibles(busqueda, pagina, tamanio, orden, estado);
    }

    @GetMapping("/opciones-factura")
    public FacturaOpcionesDTO buscarOpcionesFactura() {
        return service.buscarOpcionesFactura();
    }

    @GetMapping("/info-sistema")
    public SistemaInfoDTO buscarInfoSistema() {
        return service.buscarInfoSistema();
    }

    @GetMapping("/articulos/opciones")
    public ArticuloAltaDTO.Opciones buscarOpcionesAltaArticulo() {
        return service.buscarOpcionesAltaArticulo();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<EntidadCreadaDTO> crearUsuario(
            @Valid @RequestBody DatosMaestrosDTO.UsuarioRequest request
    ) {
        return creado(service.crearUsuario(request));
    }

    @PostMapping("/condiciones-iva")
    public ResponseEntity<EntidadCreadaDTO> crearCondicionIva(
            @Valid @RequestBody DatosMaestrosDTO.CondicionIvaRequest request
    ) {
        return creado(service.crearCondicionIva(request));
    }

    @PostMapping("/tipos-moneda")
    public ResponseEntity<EntidadCreadaDTO> crearTipoMoneda(
            @Valid @RequestBody DatosMaestrosDTO.TipoMonedaRequest request
    ) {
        return creado(service.crearTipoMoneda(request));
    }

    @PostMapping("/puntos-venta")
    public ResponseEntity<EntidadCreadaDTO> crearPuntoVenta(
            @Valid @RequestBody DatosMaestrosDTO.PuntoVentaRequest request
    ) {
        return creado(service.crearPuntoVenta(request));
    }

    @PostMapping("/clientes")
    public ResponseEntity<EntidadCreadaDTO> crearCliente(
            @Valid @RequestBody DatosMaestrosDTO.ClienteRequest request
    ) {
        return creado(service.crearCliente(request));
    }

    @PutMapping("/clientes/{clienteId}")
    public EntidadCreadaDTO actualizarCliente(
            @PathVariable Long clienteId,
            @Valid @RequestBody DatosMaestrosDTO.ClienteRequest request
    ) {
        return service.actualizarCliente(clienteId, request);
    }

    @DeleteMapping("/clientes/{clienteId}")
    public ResponseEntity<Void> desactivarCliente(
            @PathVariable Long clienteId,
            @RequestParam Long usuarioId
    ) {
        service.desactivarCliente(clienteId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/clientes/{clienteId}/lista-precio")
    public EntidadCreadaDTO actualizarListaPrecioCliente(
            @PathVariable Long clienteId,
            @Valid @RequestBody DatosMaestrosDTO.ClienteListaPrecioRequest request
    ) {
        return service.actualizarListaPrecioCliente(clienteId, request);
    }

    @PostMapping("/rubros")
    public ResponseEntity<EntidadCreadaDTO> crearRubro(
            @Valid @RequestBody DatosMaestrosDTO.RubroRequest request
    ) {
        return creado(service.crearRubro(request));
    }

    @GetMapping("/rubros")
    public PaginaDTO<CatalogoItemDTO> buscarRubros(
            @RequestParam(defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(defaultValue = "ACTIVOS") String estado
    ) {
        return service.buscarRubros(busqueda, pagina, tamanio, estado);
    }

    @PutMapping("/rubros/{rubroId}")
    public EntidadCreadaDTO actualizarRubro(
            @PathVariable Long rubroId,
            @Valid @RequestBody DatosMaestrosDTO.RubroRequest request
    ) {
        return service.actualizarRubro(rubroId, request);
    }

    @DeleteMapping("/rubros/{rubroId}")
    public ResponseEntity<Void> desactivarRubro(
            @PathVariable Long rubroId,
            @RequestParam Long usuarioId
    ) {
        service.desactivarRubro(rubroId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/marcas")
    public ResponseEntity<EntidadCreadaDTO> crearMarca(
            @Valid @RequestBody DatosMaestrosDTO.MarcaRequest request
    ) {
        return creado(service.crearMarca(request));
    }

    @GetMapping("/marcas")
    public PaginaDTO<CatalogoItemDTO> buscarMarcas(
            @RequestParam(defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(defaultValue = "ACTIVOS") String estado
    ) {
        return service.buscarMarcas(busqueda, pagina, tamanio, estado);
    }

    @PutMapping("/marcas/{marcaId}")
    public EntidadCreadaDTO actualizarMarca(
            @PathVariable Long marcaId,
            @Valid @RequestBody DatosMaestrosDTO.MarcaRequest request
    ) {
        return service.actualizarMarca(marcaId, request);
    }

    @DeleteMapping("/marcas/{marcaId}")
    public ResponseEntity<Void> desactivarMarca(
            @PathVariable Long marcaId,
            @RequestParam Long usuarioId
    ) {
        service.desactivarMarca(marcaId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/listas-precio")
    public ResponseEntity<EntidadCreadaDTO> crearListaPrecio(
            @Valid @RequestBody DatosMaestrosDTO.ListaPrecioRequest request
    ) {
        return creado(service.crearListaPrecio(request));
    }

    @PostMapping("/articulos")
    public ResponseEntity<EntidadCreadaDTO> crearArticulo(
            @Valid @RequestBody DatosMaestrosDTO.ArticuloRequest request
    ) {
        return creado(service.crearArticulo(request));
    }

    @PostMapping("/articulos-con-precios")
    public ResponseEntity<EntidadCreadaDTO> crearArticuloConPrecios(
            @Valid @RequestBody ArticuloAltaDTO.CrearRequest request
    ) {
        return creado(service.crearArticuloConPrecios(request));
    }

    @GetMapping("/articulos/{articuloId}")
    public ArticuloEdicionDTO obtenerArticulo(@PathVariable Long articuloId) {
        return service.obtenerArticuloEdicion(articuloId);
    }

    @PutMapping("/articulos/{articuloId}")
    public EntidadCreadaDTO actualizarArticulo(
            @PathVariable Long articuloId,
            @Valid @RequestBody ArticuloAltaDTO.CrearRequest request
    ) {
        return service.actualizarArticuloConPrecios(articuloId, request);
    }

    @DeleteMapping("/articulos/{articuloId}")
    public ResponseEntity<Void> desactivarArticulo(
            @PathVariable Long articuloId,
            @RequestParam Long usuarioId
    ) {
        service.desactivarArticulo(articuloId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/precios-articulo")
    public ResponseEntity<EntidadCreadaDTO> crearPrecioArticulo(
            @Valid @RequestBody DatosMaestrosDTO.ListaPrecioArticuloRequest request
    ) {
        return creado(service.crearPrecio(request));
    }

    private ResponseEntity<EntidadCreadaDTO> creado(EntidadCreadaDTO entidad) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(entidad.id())
                .toUri();
        return ResponseEntity.created(location).body(entidad);
    }
}
