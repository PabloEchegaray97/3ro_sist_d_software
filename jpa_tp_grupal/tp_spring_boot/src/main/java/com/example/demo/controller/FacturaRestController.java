package com.example.demo.controller;

import com.example.demo.dto.FacturaCreacionDTO;
import com.example.demo.dto.FacturaCreadaDTO;
import com.example.demo.dto.FacturaReporteDTO;
import com.example.demo.service.FacturaService;
import com.example.demo.service.ReporteExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@RestController
// CONSIGNA - PARTE 3.3: path base obligatorio del controlador REST.
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaRestController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final FacturaService facturaService;
    private final ReporteExportService reporteExportService;

    @PostMapping
    public ResponseEntity<FacturaCreadaDTO> crearFactura(
            @RequestBody FacturaCreacionDTO solicitud
    ) {
        FacturaCreadaDTO factura = facturaService.crearFactura(solicitud);
        return ResponseEntity.status(201).body(factura);
    }

    @GetMapping
    // CONSIGNA - PARTE 3.4 y 3.5: GET devuelve List<FacturaReporteDTO> como
    // JSON y recibe los cuatro filtros mediante @RequestParam opcionales.
    public List<FacturaReporteDTO> buscarFacturas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo
    ) {
        return facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    @GetMapping(value = "/reporte/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // CONSIGNA - PARTE 2: el PDF reutiliza exactamente la misma consulta JPQL.
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo
    ) {
        List<FacturaReporteDTO> facturas = facturaService.buscarFacturasFiltradas(
                fechaDesde, fechaHasta, estado, montoMinimo
        );
        return archivo(
                reporteExportService.generarPdf(facturas),
                MediaType.APPLICATION_PDF,
                "reporte-facturas.pdf"
        );
    }

    @GetMapping(value = "/reporte/excel")
    // CONSIGNA - PARTE 2: el Excel reutiliza exactamente la misma consulta JPQL.
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo
    ) {
        List<FacturaReporteDTO> facturas = facturaService.buscarFacturasFiltradas(
                fechaDesde, fechaHasta, estado, montoMinimo
        );
        return archivo(
                reporteExportService.generarExcel(facturas),
                EXCEL_MEDIA_TYPE,
                "reporte-facturas.xlsx"
        );
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, MediaType tipo, String nombre) {
        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(nombre, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(tipo)
                .contentLength(contenido.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(contenido);
    }
}
