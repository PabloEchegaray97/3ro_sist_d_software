package com.example.demo.service;

import com.example.demo.dto.FacturaReporteDTO;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteExportService {

    // CONSIGNA - PARTE 2: ambos archivos reciben la misma lista de
    // FacturaReporteDTO producida por la consulta JPQL solicitada.

    private static final String[] ENCABEZADOS = {
            "Numero", "Fecha", "Cliente", "Condicion IVA",
            "Punto de venta", "Importe total", "Cantidad de items"
    };

    public byte[] generarPdf(List<FacturaReporteDTO> facturas) {
        // Implementacion PDF elegida: OpenPDF.
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph encabezado = new Paragraph("Reporte ejecutivo de ventas", titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            encabezado.setSpacingAfter(14);
            documento.add(encabezado);

            PdfPTable tabla = new PdfPTable(ENCABEZADOS.length);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1.1f, 1.2f, 2.1f, 1.8f, 1.8f, 1.4f, 1.2f});

            Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String texto : ENCABEZADOS) {
                PdfPCell celda = new PdfPCell(new Phrase(texto, fuenteEncabezado));
                celda.setBackgroundColor(new Color(44, 62, 80));
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                celda.setPadding(5);
                tabla.addCell(celda);
            }

            SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
            NumberFormat formatoImporte = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
            for (FacturaReporteDTO factura : facturas) {
                tabla.addCell(String.valueOf(factura.getNumeroFactura()));
                tabla.addCell(formatoFecha.format(factura.getFechaEmision()));
                tabla.addCell(factura.getClienteDenominacion());
                tabla.addCell(factura.getCondicionIva());
                tabla.addCell(factura.getPuntoVentaDescripcion());
                tabla.addCell(formatoImporte.format(factura.getImporteTotal()));
                tabla.addCell(String.valueOf(factura.getCantidadItems()));
            }

            documento.add(tabla);
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el reporte PDF", ex);
        }
    }

    public byte[] generarExcel(List<FacturaReporteDTO> facturas) {
        // Implementacion de hoja de calculo elegida: Apache POI, formato XLSX real.
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Facturas");
            CellStyle estiloEncabezado = libro.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            estiloEncabezado.setFont(fuenteEncabezado);

            CreationHelper creationHelper = libro.getCreationHelper();
            CellStyle estiloFecha = libro.createCellStyle();
            estiloFecha.setDataFormat(creationHelper.createDataFormat().getFormat("dd/mm/yyyy"));

            CellStyle estiloImporte = libro.createCellStyle();
            estiloImporte.setDataFormat(creationHelper.createDataFormat().getFormat("$ #,##0.00"));

            Row encabezado = hoja.createRow(0);
            for (int columna = 0; columna < ENCABEZADOS.length; columna++) {
                Cell celda = encabezado.createCell(columna);
                celda.setCellValue(ENCABEZADOS[columna]);
                celda.setCellStyle(estiloEncabezado);
            }

            int numeroFila = 1;
            for (FacturaReporteDTO factura : facturas) {
                Row fila = hoja.createRow(numeroFila++);
                fila.createCell(0).setCellValue(factura.getNumeroFactura());

                Cell fecha = fila.createCell(1);
                fecha.setCellValue(factura.getFechaEmision());
                fecha.setCellStyle(estiloFecha);

                fila.createCell(2).setCellValue(factura.getClienteDenominacion());
                fila.createCell(3).setCellValue(factura.getCondicionIva());
                fila.createCell(4).setCellValue(factura.getPuntoVentaDescripcion());

                Cell importe = fila.createCell(5);
                importe.setCellValue(factura.getImporteTotal());
                importe.setCellStyle(estiloImporte);

                fila.createCell(6).setCellValue(factura.getCantidadItems());
            }

            for (int columna = 0; columna < ENCABEZADOS.length; columna++) {
                hoja.autoSizeColumn(columna);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el reporte Excel", ex);
        }
    }
}
