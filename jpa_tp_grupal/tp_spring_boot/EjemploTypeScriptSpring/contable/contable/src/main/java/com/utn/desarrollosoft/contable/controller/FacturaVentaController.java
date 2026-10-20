package com.utn.desarrollosoft.contable.controller;

import com.utn.desarrollosoft.contable.entidades.dto.FacturaVentaDTO;
import com.utn.desarrollosoft.contable.servicios.FacturaVentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class FacturaVentaController {

    private final FacturaVentaService facturaVentaService;

    public FacturaVentaController(FacturaVentaService facturaVentaService) {
        this.facturaVentaService = facturaVentaService;
    }

    @RequestMapping(value = "prueba")
    public String prueba() {
        return "PRUEBA SPRING BOOT";
    }

    @GetMapping("prueba2")
    public String prueba2() {
        return "PRUEBA 2 SPRING BOOT";
    }

    @GetMapping("api/facturas")
    public ResponseEntity<List<FacturaVentaDTO>> getFacturasVenta(
            @RequestParam(required = false) Long numeroFactura,
            @RequestParam(required = false) String clienteDenominacion,
            @RequestParam(required = false) String condicionIva,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta) {

        List<FacturaVentaDTO> facturas = facturaVentaService.filtrar(
                numeroFactura, clienteDenominacion, condicionIva, fechaDesde, fechaHasta);
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("api/facturas/{numeroFactura}")
    public ResponseEntity<FacturaVentaDTO> getFacturaPorNumero(@PathVariable Long numeroFactura) {
        return facturaVentaService.buscarPorNumero(numeroFactura)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("api/facturas")
    public ResponseEntity<FacturaVentaDTO> crearFactura(@RequestBody FacturaVentaDTO factura) {
        FacturaVentaDTO creada = facturaVentaService.guardar(factura);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("api/facturas/{numeroFactura}")
    public ResponseEntity<FacturaVentaDTO> actualizarFactura(
            @PathVariable Long numeroFactura,
            @RequestBody FacturaVentaDTO factura) {
        return facturaVentaService.actualizar(numeroFactura, factura)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("api/facturas/{numeroFactura}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long numeroFactura) {
        boolean eliminada = facturaVentaService.eliminar(numeroFactura);
        if (eliminada) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
