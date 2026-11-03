package com.example.demo.controller;

import com.example.demo.dto.FacturaReporteDTO;
import com.example.demo.dto.FacturaRequestDTO;
import com.example.demo.dto.FacturaVistaDTO;
import com.example.demo.dto.ArcaEmisionDTO;
import com.example.demo.dto.ArcaConfiguracionDTO;
import com.example.demo.dto.ArcaAutenticacionDTO;
import jakarta.validation.Valid;
import com.example.demo.service.ArcaHomologacionService;
import com.example.demo.service.FacturaService;
import com.example.demo.service.ReporteExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
// CONSIGNA - PARTE 3.3: path base obligatorio del controlador REST.
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
// CONSIGNA ESCRITURA - 4.b: permite consumir la API desde el frontend local de Vite.
@CrossOrigin(origins = "http://localhost:5173")
public class FacturaRestController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final FacturaService facturaService;
    private final ReporteExportService reporteExportService;
    private final ArcaHomologacionService arcaHomologacionService;

    // CONSIGNA ESCRITURA - 3.1: POST recibe Request DTO, crea en cascada
    // y responde 201 Created con FacturaReporteDTO.
    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crearFactura(
            @Valid @RequestBody FacturaRequestDTO solicitud
    ) {
        FacturaReporteDTO factura = facturaService.crear(solicitud);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(factura.getId())
                .toUri();
        return ResponseEntity.created(location).body(factura);
    }

    // CONSIGNA ESCRITURA - 3.1: PUT actualiza cabecera, detalles o solamente
    // estado/observaciones y responde 200 OK con el DTO actualizado.
    @PutMapping("/{id}")
    public ResponseEntity<FacturaReporteDTO> actualizarFactura(
            @PathVariable Long id,
            @Valid @RequestBody FacturaRequestDTO solicitud
    ) {
        return ResponseEntity.ok(facturaService.actualizar(id, solicitud));
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<FacturaReporteDTO> anularFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.anular(id));
    }

    // CONSIGNA ESCRITURA - 3.1: DELETE realiza eliminacion fisica y retorna 204.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    // CONSIGNA - PARTE 3.4 y 3.5: GET devuelve List<FacturaReporteDTO> como
    // JSON y recibe los cuatro filtros mediante @RequestParam opcionales.
    public List<FacturaReporteDTO> buscarFacturas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) BigDecimal montoMinimo
    ) {
        return facturaService.buscarFacturasFiltradas(desde(fechaDesde), hasta(fechaHasta), estado, montoMinimo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaVistaDTO> obtenerFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtenerPorId(id));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> imprimirFactura(@PathVariable Long id) {
        FacturaVistaDTO factura = facturaService.obtenerPorId(id);
        if (factura.getNumero() == null || factura.getCae() == null) {
            throw new com.example.demo.exception.ConflictoNegocioException(
                    "Solo se puede imprimir un comprobante emitido con CAE"
            );
        }
        byte[] contenido = reporteExportService.generarComprobantePdf(factura);
        String nombre = "factura-c-" + factura.getNumero() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(contenido.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(nombre, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(contenido);
    }

    @PostMapping("/{id}/emitir")
    public ResponseEntity<ArcaEmisionDTO> emitirFactura(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.emitirConArca(id));
    }

    @GetMapping("/arca/configuracion")
    public ArcaConfiguracionDTO obtenerConfiguracionArca() {
        return arcaHomologacionService.configuracion();
    }

    @PostMapping("/arca/probar-autenticacion")
    public ArcaAutenticacionDTO probarAutenticacionArca() {
        return arcaHomologacionService.probarAutenticacion();
    }

    @GetMapping("/totales-por-moneda")
    public Map<String, BigDecimal> obtenerTotalesPorMoneda() {
        return facturaService.totalesEmitidosPorMoneda();
    }

    @GetMapping(value = "/reporte/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    // CONSIGNA - PARTE 2: el PDF reutiliza exactamente la misma consulta JPQL.
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) BigDecimal montoMinimo
    ) {
        List<FacturaReporteDTO> facturas = facturaService.buscarFacturasFiltradas(
                desde(fechaDesde), hasta(fechaHasta), estado, montoMinimo
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
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) BigDecimal montoMinimo
    ) {
        List<FacturaReporteDTO> facturas = facturaService.buscarFacturasFiltradas(
                desde(fechaDesde), hasta(fechaHasta), estado, montoMinimo
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

    private Date desde(LocalDate fecha) {
        return fecha == null ? null : Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date hasta(LocalDate fecha) {
        return fecha == null ? null : Date.from(
                fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1)
        );
    }
}
