package com.example.demo.service;

import com.example.demo.config.ArcaHomologacionProperties;
import com.example.demo.dto.FacturaReporteDTO;
import com.example.demo.dto.FacturaDetalleVistaDTO;
import com.example.demo.dto.FacturaVistaDTO;
import com.example.demo.exception.PrecisionMonetariaException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.util.Base64;

@Service
public class ReporteExportService {

    private final ArcaHomologacionProperties arcaProperties;
    private final FacturaTemplatePdfService facturaTemplatePdfService;

    public ReporteExportService(ArcaHomologacionProperties arcaProperties,
                                FacturaTemplatePdfService facturaTemplatePdfService) {
        this.arcaProperties = arcaProperties;
        this.facturaTemplatePdfService = facturaTemplatePdfService;
    }

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
                tabla.addCell(factura.getNumeroFactura() == null ? "Sin emitir" : String.valueOf(factura.getNumeroFactura()));
                tabla.addCell(factura.getFechaEmision() == null ? "Sin emitir" : formatoFecha.format(factura.getFechaEmision()));
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

    public byte[] generarComprobantePdf(FacturaVistaDTO factura) {
        return facturaTemplatePdfService.generar(factura);
    }

    private byte[] generarComprobantePdfLegacy(FacturaVistaDTO factura) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4, 28, 28, 24, 24);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font letra = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font etiqueta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font valor = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font auxiliar = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(71, 85, 105));
            Font encabezadoTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
            Font cuerpoTabla = FontFactory.getFont(FontFactory.HELVETICA, 7);
            NumberFormat importe = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
            SimpleDateFormat fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            SimpleDateFormat fecha = new SimpleDateFormat("dd/MM/yyyy");

            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            PdfPCell bannerCell = celda("ORIGINAL · HOMOLOGACIÓN", etiqueta, Element.ALIGN_CENTER);
            bannerCell.setBackgroundColor(new Color(242, 244, 248));
            bannerCell.setPadding(5);
            banner.addCell(bannerCell);
            banner.setSpacingAfter(0);
            documento.add(banner);

            PdfPTable cabecera = new PdfPTable(3);
            cabecera.setWidthPercentage(100);
            cabecera.setWidths(new float[]{46, 9, 45});

            PdfPCell emisor = new PdfPCell();
            emisor.setPadding(10);
            emisor.setMinimumHeight(112);
            PdfPTable identidad = new PdfPTable(2);
            identidad.setWidthPercentage(100);
            identidad.setWidths(new float[]{1, 3});
            PdfPCell logo = celdaLogo();
            identidad.addCell(logo);
            PdfPCell nombre = new PdfPCell(new Phrase(valor(arcaProperties.getRazonSocialEmisor(), "Monotributista"), titulo));
            nombre.setBorder(0);
            nombre.setPaddingLeft(8);
            nombre.setVerticalAlignment(Element.ALIGN_MIDDLE);
            identidad.addCell(nombre);
            emisor.addElement(identidad);
            agregarLinea(emisor, "Razón social", arcaProperties.getRazonSocialEmisor(), etiqueta, valor);
            agregarLinea(emisor, "Condición IVA", arcaProperties.getCondicionIvaEmisor(), etiqueta, valor);
            agregarLinea(emisor, "Domicilio comercial", factura.getPuntoVentaDomicilio(), etiqueta, valor);
            agregarLinea(emisor, "Teléfono", arcaProperties.getTelefonoEmisor(), etiqueta, valor);
            cabecera.addCell(emisor);

            PdfPCell clase = new PdfPCell(new Phrase(letraComprobante(factura.getTipoComprobante()), letra));
            clase.setHorizontalAlignment(Element.ALIGN_CENTER);
            clase.setVerticalAlignment(Element.ALIGN_MIDDLE);
            clase.setBackgroundColor(Color.WHITE);
            cabecera.addCell(clase);

            PdfPCell comprobante = new PdfPCell();
            comprobante.setPadding(10);
            comprobante.setMinimumHeight(112);
            Paragraph facturaTitulo = new Paragraph("FACTURA", titulo);
            facturaTitulo.setAlignment(Element.ALIGN_RIGHT);
            comprobante.addElement(facturaTitulo);
            agregarLinea(comprobante, "Punto de venta", String.format("%05d", factura.getPuntoVentaNumero()), etiqueta, valor);
            agregarLinea(comprobante, "Comprobante N°", factura.getNumero() == null
                    ? "Sin emitir" : String.format("%08d", factura.getNumero()), etiqueta, valor);
            agregarLinea(comprobante, "Fecha de emisión", factura.getFechaEmision() == null
                    ? "Sin emitir" : fechaHora.format(factura.getFechaEmision()), etiqueta, valor);
            agregarLinea(comprobante, "CUIT", arcaProperties.getCuit(), etiqueta, valor);
            agregarLinea(comprobante, "Ingresos Brutos", arcaProperties.getIngresosBrutosEmisor(), etiqueta, valor);
            agregarLinea(comprobante, "Inicio de actividades",
                    LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (TEST)",
                    etiqueta, valor);
            cabecera.addCell(comprobante);
            cabecera.setSpacingAfter(10);
            documento.add(cabecera);

            PdfPTable cliente = new PdfPTable(2);
            cliente.setWidthPercentage(100);
            cliente.setWidths(new float[]{3, 2});
            PdfPCell clientePrincipal = new PdfPCell();
            clientePrincipal.setPadding(9);
            agregarLinea(clientePrincipal, "Cliente", factura.getClienteDenominacion(), etiqueta, valor);
            agregarLinea(clientePrincipal, "CUIT/CUIL", factura.getClienteId() == null
                    ? "Consumidor Final" : factura.getClienteCuitCuil(), etiqueta, valor);
            agregarLinea(clientePrincipal, "Domicilio", factura.getClienteId() == null
                    ? "—" : factura.getClienteDomicilio(), etiqueta, valor);
            cliente.addCell(clientePrincipal);
            PdfPCell clienteContacto = new PdfPCell();
            clienteContacto.setPadding(9);
            agregarLinea(clienteContacto, "Condición fiscal", factura.getCondicionIva(), etiqueta, valor);
            agregarLinea(clienteContacto, "Teléfono", factura.getClienteId() == null
                    ? "—" : factura.getClienteTelefono(), etiqueta, valor);
            agregarLinea(clienteContacto, "Email", factura.getClienteId() == null
                    ? "—" : factura.getClienteEmail(), etiqueta, valor);
            cliente.addCell(clienteContacto);
            cliente.setSpacingAfter(10);
            documento.add(cliente);

            if (factura.getObservaciones() != null && !factura.getObservaciones().isBlank()) {
                Paragraph observaciones = new Paragraph();
                observaciones.add(new Phrase("Observaciones: ", etiqueta));
                observaciones.add(new Phrase(factura.getObservaciones(), valor));
                observaciones.setSpacingAfter(8);
                documento.add(observaciones);
            }

            String[] columnas = {"Código", "Artículo / descripción", "Cantidad", "Precio neto", "IVA", "Precio unitario", "Subtotal"};
            PdfPTable detalles = new PdfPTable(columnas.length);
            detalles.setWidthPercentage(100);
            detalles.setWidths(new float[]{1.05f, 2.7f, .75f, 1.05f, .65f, 1.05f, 1.1f});
            for (String columna : columnas) {
                PdfPCell celda = celda(columna, encabezadoTabla, Element.ALIGN_CENTER);
                celda.setBackgroundColor(new Color(36, 44, 79));
                detalles.addCell(celda);
            }
            for (FacturaDetalleVistaDTO detalle : factura.getDetalles()) {
                detalles.addCell(celda(detalle.getArticuloCodigo(), cuerpoTabla, Element.ALIGN_LEFT));
                String descripcion = detalle.getArticuloDenominacion();
                if (detalle.getDescripcion() != null && !detalle.getDescripcion().isBlank()) {
                    descripcion += "\n" + detalle.getDescripcion();
                }
                detalles.addCell(celda(descripcion, cuerpoTabla, Element.ALIGN_LEFT));
                detalles.addCell(celda(numero(detalle.getCantidad()), cuerpoTabla, Element.ALIGN_RIGHT));
                detalles.addCell(celda(importe.format(detalle.getImporteNeto()), cuerpoTabla, Element.ALIGN_RIGHT));
                detalles.addCell(celda(numero(detalle.getPorcentajeIva()) + " %", cuerpoTabla, Element.ALIGN_RIGHT));
                detalles.addCell(celda(importe.format(detalle.getPrecioUnitario()), cuerpoTabla, Element.ALIGN_RIGHT));
                detalles.addCell(celda(importe.format(detalle.getImporteSubtotal()), cuerpoTabla, Element.ALIGN_RIGHT));
            }
            PdfPCell espacio = new PdfPCell(new Phrase(""));
            espacio.setColspan(columnas.length);
            espacio.setFixedHeight(90);
            espacio.setBorderColor(new Color(226, 232, 240));
            detalles.addCell(espacio);
            detalles.setSpacingAfter(10);
            documento.add(detalles);

            BigDecimal neto = factura.getDetalles().stream()
                    .map(FacturaDetalleVistaDTO::getImporteNeto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal iva = factura.getDetalles().stream()
                    .map(FacturaDetalleVistaDTO::getImporteIva)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            PdfPTable totales = new PdfPTable(2);
            totales.setWidthPercentage(38);
            totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            agregarTotal(totales, "Neto", importe.format(neto), etiqueta, valor);
            agregarTotal(totales, "IVA", importe.format(iva), etiqueta, valor);
            agregarTotal(totales, "TOTAL", importe.format(factura.getImporteTotal()), subtitulo, subtitulo);
            totales.setSpacingAfter(12);
            documento.add(totales);

            PdfPTable pie = new PdfPTable(2);
            pie.setWidthPercentage(100);
            pie.setWidths(new float[]{3, 2});
            PdfPCell pieIzquierdo = new PdfPCell();
            pieIzquierdo.setPadding(9);
            agregarLinea(pieIzquierdo, "Imprimió", factura.getUsuario(), etiqueta, valor);
            agregarLinea(pieIzquierdo, "Emisor", arcaProperties.getRazonSocialEmisor(), etiqueta, valor);
            agregarLinea(pieIzquierdo, "CUIT", arcaProperties.getCuit(), etiqueta, valor);
            pieIzquierdo.addElement(new Paragraph("Documento generado en ambiente de homologación.", auxiliar));
            pie.addCell(pieIzquierdo);
            PdfPCell pieFiscal = new PdfPCell();
            pieFiscal.setPadding(9);
            if (factura.getCae() == null || factura.getCae().isBlank()) {
                Paragraph sinValor = new Paragraph("DOCUMENTO NO VÁLIDO COMO FACTURA", subtitulo);
                sinValor.setAlignment(Element.ALIGN_CENTER);
                pieFiscal.addElement(sinValor);
                pieFiscal.addElement(new Paragraph("El comprobante todavía no tiene CAE.", auxiliar));
            } else {
                agregarLinea(pieFiscal, "CAE", factura.getCae(), etiqueta, valor);
                agregarLinea(pieFiscal, "Vencimiento CAE", factura.getCaeFechaVencimiento() == null
                        ? "—" : fecha.format(factura.getCaeFechaVencimiento()), etiqueta, valor);
                agregarLinea(pieFiscal, "Resultado ARCA", resultadoArca(factura.getResultadoAfip()), etiqueta, valor);
                Image qr = imagenQr(urlQrArca(factura));
                qr.scaleAbsolute(78, 78);
                qr.setAlignment(Element.ALIGN_RIGHT);
                pieFiscal.addElement(qr);
            }
            pie.addCell(pieFiscal);
            documento.add(pie);

            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el comprobante PDF", ex);
        }
    }

    private void agregarLinea(PdfPCell celda, String nombre, String contenido, Font etiqueta, Font valor) {
        Paragraph linea = new Paragraph();
        linea.setLeading(11);
        linea.add(new Phrase(nombre + ": ", etiqueta));
        linea.add(new Phrase(valor(contenido, "—"), valor));
        celda.addElement(linea);
    }

    private String letraComprobante(int tipoComprobante) {
        if (tipoComprobante == 1) return "A";
        if (tipoComprobante == 6) return "B";
        if (tipoComprobante == 11) return "C";
        return String.valueOf(tipoComprobante);
    }

    private String urlQrArca(FacturaVistaDTO factura) {
        SimpleDateFormat fechaQr = new SimpleDateFormat("yyyy-MM-dd");
        String cuit = digitos(arcaProperties.getCuit());
        String documento = factura.getClienteId() == null ? "0" : digitos(factura.getClienteCuitCuil());
        int tipoDocumento = factura.getClienteId() == null ? 99 : 80;
        String json = "{" +
                "\"ver\":1," +
                "\"fecha\":\"" + fechaQr.format(factura.getFechaEmision()) + "\"," +
                "\"cuit\":" + cuit + "," +
                "\"ptoVta\":" + factura.getPuntoVentaNumero() + "," +
                "\"tipoCmp\":" + factura.getTipoComprobante() + "," +
                "\"nroCmp\":" + factura.getNumero() + "," +
                "\"importe\":" + factura.getImporteTotal().setScale(2, RoundingMode.HALF_UP).toPlainString() + "," +
                "\"moneda\":\"" + factura.getTipoMonedaCodigo() + "\"," +
                "\"ctz\":1," +
                "\"tipoDocRec\":" + tipoDocumento + "," +
                "\"nroDocRec\":" + documento + "," +
                "\"tipoCodAut\":\"E\"," +
                "\"codAut\":" + digitos(factura.getCae()) +
                "}";
        String payload = Base64.getEncoder()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return "https://www.arca.gob.ar/fe/qr/?p="
                + URLEncoder.encode(payload, StandardCharsets.UTF_8);
    }

    private Image imagenQr(String contenido) throws DocumentException, IOException {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 240, 240);
            BufferedImage imagen = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 240; y++) {
                for (int x = 0; x < 240; x++) {
                    imagen.setRGB(x, y, matriz.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            try (ByteArrayOutputStream png = new ByteArrayOutputStream()) {
                ImageIO.write(imagen, "png", png);
                return Image.getInstance(png.toByteArray());
            }
        } catch (com.google.zxing.WriterException ex) {
            throw new IOException("No se pudo generar el QR fiscal", ex);
        }
    }

    private PdfPCell celdaLogo() throws DocumentException, IOException {
        String logoPath = arcaProperties.getLogoPath();
        PdfPCell celda;
        if (logoPath != null && !logoPath.isBlank() && Files.isRegularFile(Path.of(logoPath))) {
            Image imagen = Image.getInstance(logoPath);
            imagen.scaleToFit(58, 40);
            celda = new PdfPCell(imagen, true);
        } else {
            celda = new PdfPCell(new Phrase(""));
        }
        celda.setFixedHeight(44);
        celda.setPadding(2);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setBorderColor(new Color(203, 213, 225));
        return celda;
    }

    private String digitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    private void agregarTotal(PdfPTable tabla, String nombre, String contenido, Font etiqueta, Font valor) {
        tabla.addCell(celda(nombre, etiqueta, Element.ALIGN_RIGHT));
        tabla.addCell(celda(contenido, valor, Element.ALIGN_RIGHT));
    }

    private PdfPCell celda(String contenido, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(valor(contenido, ""), fuente));
        celda.setPadding(6);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setBorderColor(new Color(226, 232, 240));
        return celda;
    }

    private String valor(String contenido, String alternativa) {
        return contenido == null || contenido.isBlank() ? alternativa : contenido;
    }

    private String numero(BigDecimal valor) {
        BigDecimal normalizado = valor.stripTrailingZeros();
        return normalizado.scale() <= 0 ? normalizado.toPlainString() : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String resultadoArca(String resultado) {
        if ("A".equalsIgnoreCase(resultado)) return "A: Aprobada con CAE";
        if ("R".equalsIgnoreCase(resultado)) return "R: Rechazada por ARCA";
        return valor(resultado, "Sin resultado");
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
                fila.createCell(0).setCellValue(factura.getNumeroFactura() == null ? "" : String.valueOf(factura.getNumeroFactura()));

                Cell fecha = fila.createCell(1);
                if (factura.getFechaEmision() != null) fecha.setCellValue(factura.getFechaEmision());
                fecha.setCellStyle(estiloFecha);

                fila.createCell(2).setCellValue(factura.getClienteDenominacion());
                fila.createCell(3).setCellValue(factura.getCondicionIva());
                fila.createCell(4).setCellValue(factura.getPuntoVentaDescripcion());

                Cell importe = fila.createCell(5);
                importe.setCellValue(doubleExacto(factura.getImporteTotal()));
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

    private double doubleExacto(BigDecimal valor) {
        BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_UP);
        double convertido = normalizado.doubleValue();
        if (!Double.isFinite(convertido)
                || BigDecimal.valueOf(convertido).setScale(2, RoundingMode.HALF_UP).compareTo(normalizado) != 0) {
            throw new PrecisionMonetariaException("El importe " + valor.toPlainString()
                    + " no puede exportarse a XLSX sin perder centavos");
        }
        return convertido;
    }
}
