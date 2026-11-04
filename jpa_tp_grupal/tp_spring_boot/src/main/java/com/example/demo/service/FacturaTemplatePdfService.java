package com.example.demo.service;

import com.example.demo.config.ArcaHomologacionProperties;
import com.example.demo.dto.FacturaDetalleVistaDTO;
import com.example.demo.dto.FacturaVistaDTO;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FacturaTemplatePdfService {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private final ArcaHomologacionProperties properties;
    private final Template template;

    public FacturaTemplatePdfService(ArcaHomologacionProperties properties) {
        this.properties = properties;
        try {
            ClassPathTemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
            this.template = new Handlebars(loader).compile("factura");
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar la plantilla Handlebars de factura", ex);
        }
    }

    public byte[] generar(FacturaVistaDTO factura) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            String html = template.apply(modelo(factura));
            new PdfRendererBuilder()
                    .useSVGDrawer(new BatikSVGDrawer())
                    .withHtmlContent(html, null)
                    .toStream(salida)
                    .run();
            return salida.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el comprobante desde la plantilla Handlebars", ex);
        }
    }

    private Map<String, Object> modelo(FacturaVistaDTO factura) throws Exception {
        boolean autorizado = factura.getCae() != null && !factura.getCae().isBlank();
        Map<String, Object> model = new LinkedHashMap<>();
        String logoDataUri = logoDataUri();
        String arcaLogoDataUri = classpathDataUri("static/invoice/logo_afip.png", "image/png");
        model.put("hasLogo", !logoDataUri.isBlank());
        model.put("logoDataUri", logoDataUri);
        model.put("hasArcaLogo", !arcaLogoDataUri.isBlank());
        model.put("arcaLogoDataUri", arcaLogoDataUri);
        model.put("issuer", map(
                "tradeName", valor(properties.getRazonSocialEmisor(), "Test Facturas"),
                "legalName", valor(properties.getRazonSocialEmisor(), "Test Facturas"),
                "address", valor(factura.getPuntoVentaDomicilio(), "No informado (TEST)"),
                "ivaCondition", condicionIvaEmisor(),
                "phone", valor(properties.getTelefonoEmisor(), "2615000000"),
                "cuit", valor(properties.getCuit(), "No configurado"),
                "grossIncome", valor(properties.getIngresosBrutosEmisor(), "No contribuyente (TEST)"),
                "activityStart", LocalDate.now(ZONA_ARGENTINA).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (TEST)"
        ));
        String puntoVenta = String.format("%05d", factura.getPuntoVentaNumero());
        String numeroComprobante = factura.getNumero() == null ? "Sin emitir" : String.format("%08d", factura.getNumero());
        model.put("voucher", map(
                "letter", letra(factura.getTipoComprobante()),
                "code", String.format("%03d", factura.getTipoComprobante()),
                "pointOfSale", puntoVenta,
                "number", numeroComprobante,
                "fullNumber", puntoVenta + "-" + numeroComprobante,
                "date", factura.getFechaEmision() == null ? "Sin emitir" : new SimpleDateFormat("dd/MM/yyyy").format(factura.getFechaEmision())
        ));
        BigDecimal saldo = factura.getImporteSaldo() == null ? BigDecimal.ZERO : factura.getImporteSaldo();
        BigDecimal total = factura.getImporteTotal() == null ? BigDecimal.ZERO : factura.getImporteTotal();
        model.put("receiver", map(
                "documentLabel", factura.getClienteId() == null
                        ? "Documento"
                        : valor(factura.getClienteTipoDocumento(), "CUIT/CUIL"),
                "document", factura.getClienteId() == null ? "NR" : valor(factura.getClienteCuitCuil(), "NR"),
                "name", factura.getClienteId() == null ? "Consumidor Final" : valor(factura.getClienteDenominacion(), "Consumidor Final"),
                "ivaCondition", factura.getClienteId() == null
                        ? "A CONSUMIDOR FINAL"
                        : valor(factura.getCondicionIva(), "Consumidor Final"),
                "phone", factura.getClienteId() == null ? "—" : valor(factura.getClienteTelefono(), "—"),
                "saleCondition", saldo.signum() == 0 ? "Contado" : "Cuenta corriente",
                "address", factura.getClienteId() == null ? "—" : valor(factura.getClienteDomicilio(), "—")
        ));
        model.put("items", items(factura));
        model.put("totals", map(
                "subtotal", importe(total),
                "otherTaxes", importe(BigDecimal.ZERO),
                "total", importe(total)
        ));
        model.put("fiscalTransparency", map(
                "vatIncluded", importe(ivaContenido(factura)),
                "otherNationalTaxes", importe(BigDecimal.ZERO)
        ));
        model.put("currency", map(
                "code", valor(factura.getTipoMonedaCodigo(), "PES"),
                "exchangeRate", "1,000000"
        ));
        model.put("authorized", autorizado);
        model.put("cae", autorizado ? factura.getCae() : "");
        model.put("caeExpiration", autorizado && factura.getCaeFechaVencimiento() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(factura.getCaeFechaVencimiento()) : "");
        model.put("qrDataUri", autorizado ? qrDataUri(urlQrArca(factura)) : "");
        return model;
    }

    private List<Map<String, Object>> items(FacturaVistaDTO factura) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (factura.getDetalles() == null) return items;
        for (FacturaDetalleVistaDTO detalle : factura.getDetalles()) {
            BigDecimal precioUnitario = nuloComoCero(detalle.getPrecioUnitario());
            BigDecimal cantidad = nuloComoCero(detalle.getCantidad());
            BigDecimal porcentajeBonificacion = nuloComoCero(detalle.getPorcentajeBonificacion());
            BigDecimal precioUnitarioMostrado = factura.getTipoComprobante() == 11
                    ? precioUnitario.multiply(BigDecimal.ONE.add(
                            nuloComoCero(detalle.getPorcentajeIva())
                                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                    )).setScale(2, RoundingMode.HALF_UP)
                    : precioUnitario;
            BigDecimal bonificacion = precioUnitarioMostrado
                    .multiply(cantidad)
                    .multiply(porcentajeBonificacion)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            String descripcion = valor(detalle.getArticuloDenominacion(), "Artículo");
            if (detalle.getDescripcion() != null && !detalle.getDescripcion().isBlank()) {
                descripcion += " · " + detalle.getDescripcion();
            }
            items.add(map(
                    "code", detalle.getArticuloCodigo(),
                    "description", descripcion,
                    "quantity", numero(cantidad),
                    "unit", "unidad",
                    "unitPrice", importe(precioUnitarioMostrado),
                    "discountPercent", numero(porcentajeBonificacion) + " %",
                    "discountAmount", importe(bonificacion),
                    "subtotal", importe(nuloComoCero(detalle.getImporteSubtotal()))
            ));
        }
        return items;
    }

    private String urlQrArca(FacturaVistaDTO factura) {
        String json = "{" +
                "\"ver\":1," +
                "\"fecha\":\"" + new SimpleDateFormat("yyyy-MM-dd").format(factura.getFechaEmision()) + "\"," +
                "\"cuit\":" + digitos(properties.getCuit()) + "," +
                "\"ptoVta\":" + factura.getPuntoVentaNumero() + "," +
                "\"tipoCmp\":" + factura.getTipoComprobante() + "," +
                "\"nroCmp\":" + factura.getNumero() + "," +
                "\"importe\":" + factura.getImporteTotal().setScale(2, RoundingMode.HALF_UP).toPlainString() + "," +
                "\"moneda\":\"" + factura.getTipoMonedaCodigo() + "\"," +
                "\"ctz\":1," +
                "\"tipoDocRec\":" + tipoDocumentoReceptor(factura) + "," +
                "\"nroDocRec\":" + (factura.getClienteId() == null ? "0" : digitos(factura.getClienteCuitCuil())) + "," +
                "\"tipoCodAut\":\"E\"," +
                "\"codAut\":" + digitos(factura.getCae()) + "}";
        String payload = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return "https://www.arca.gob.ar/fe/qr/?p=" + URLEncoder.encode(payload, StandardCharsets.UTF_8);
    }

    private int tipoDocumentoReceptor(FacturaVistaDTO factura) {
        if (factura.getClienteId() == null) return 99;
        return "CUIL".equalsIgnoreCase(factura.getClienteTipoDocumento()) ? 86 : 80;
    }

    private String qrDataUri(String contenido) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 240, 240);
        BufferedImage image = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 240; y++) {
            for (int x = 0; x < 240; x++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        try (ByteArrayOutputStream png = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", png);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        }
    }

    private String logoDataUri() throws IOException {
        return classpathDataUri("static/invoice/react.svg", "image/svg+xml");
    }

    private String classpathDataUri(String resourcePath, String mediaType) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) return "";
        try (var input = resource.getInputStream()) {
            return "data:" + mediaType + ";base64,"
                    + Base64.getEncoder().encodeToString(input.readAllBytes());
        }
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    private String importe(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("es", "AR")));
        return format.format(value.setScale(2, RoundingMode.HALF_UP));
    }

    private String numero(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.toPlainString();
    }

    private BigDecimal nuloComoCero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal ivaContenido(FacturaVistaDTO factura) {
        // En los comprobantes C el importe de IVA informado a WSFE debe ser cero.
        if (factura.getTipoComprobante() == 11 || factura.getDetalles() == null) return BigDecimal.ZERO;
        return factura.getDetalles().stream()
                .map(FacturaDetalleVistaDTO::getImporteIva)
                .map(this::nuloComoCero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String letra(int type) {
        if (type == 1) return "A";
        if (type == 6) return "B";
        if (type == 11) return "C";
        return String.valueOf(type);
    }

    private String valor(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String condicionIvaEmisor() {
        String condicion = valor(properties.getCondicionIvaEmisor(), "Monotributo");
        if ("MONOTRIBUTO".equalsIgnoreCase(condicion)) return "RESPONSABLE MONOTRIBUTO";
        return condicion;
    }

    private String digitos(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
