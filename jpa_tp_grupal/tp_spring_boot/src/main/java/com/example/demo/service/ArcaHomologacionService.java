package com.example.demo.service;

import com.example.demo.config.ArcaHomologacionProperties;
import com.example.demo.dto.ArcaConfiguracionDTO;
import com.example.demo.dto.ArcaEmisionDTO;
import com.example.demo.dto.ArcaAutenticacionDTO;
import com.example.demo.model.Cliente;
import com.example.demo.model.FacturaVenta;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArcaHomologacionService {

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String WSFE_NS = "http://ar.gov.afip.dif.FEV1/";
    private static final DateTimeFormatter ARCA_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ArcaHomologacionProperties properties;
    private volatile Ticket cachedTicket;

    public ArcaConfiguracionDTO configuracion() {
        String cuit = properties.getCuit();
        String keystorePath = properties.getKeystorePath();
        boolean cuitConfigurado = digitos(cuit).length() == 11;
        boolean keystoreConfigurado = keystorePath != null && !keystorePath.isBlank();
        boolean keystoreExiste = keystoreConfigurado
                && Files.isRegularFile(Path.of(keystorePath));
        boolean passwordConfigurada = properties.getKeystorePassword() != null
                && !properties.getKeystorePassword().isBlank();
        boolean endpointsHomologacion = esEndpointHomologacion(
                properties.getWsaaUrl(),
                "wsaahomo.afip.gov.ar"
        ) && esEndpointHomologacion(
                properties.getWsfeUrl(),
                "wswhomo.afip.gov.ar"
        );

        return new ArcaConfiguracionDTO(
                "HOMOLOGACION",
                cuit,
                properties.getCondicionIvaEmisor(),
                properties.getRazonSocialEmisor(),
                properties.getIngresosBrutosEmisor(),
                LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (TEST)",
                properties.getTelefonoEmisor(),
                properties.getLogoPath(),
                tipoComprobante(),
                "Factura C",
                cuitConfigurado,
                keystorePath,
                keystoreConfigurado,
                keystoreExiste,
                passwordConfigurada,
                properties.getWsaaUrl(),
                properties.getWsfeUrl(),
                endpointsHomologacion,
                cuitConfigurado && keystoreExiste && passwordConfigurada
                        && endpointsHomologacion
        );
    }

    public ArcaAutenticacionDTO probarAutenticacion() {
        validarConfiguracion();
        try {
            Ticket current = ticket();
            return new ArcaAutenticacionDTO(true, "HOMOLOGACION", "wsfe", current.expiration(),
                    "TRA firmado y ticket de acceso obtenido correctamente");
        } catch (Exception ex) {
            throw new ArcaException("No se pudo autenticar contra WSAA: " + mensaje(ex), ex);
        }
    }

    public ResultadoArca emitir(FacturaVenta factura, Date fechaComprobante) {
        validarConfiguracion();
        try {
            Ticket ticket = ticket();
            long cuit = Long.parseLong(digitos(properties.getCuit()));
            int puntoVenta = factura.getPuntoVenta().getNumero();
            int tipoComprobante = tipoComprobante();
            long numero = ultimoAutorizado(ticket, cuit, puntoVenta, tipoComprobante) + 1;
            return solicitarCae(ticket, cuit, factura, fechaComprobante, puntoVenta, tipoComprobante, numero);
        } catch (ArcaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ArcaException("No se pudo completar la comunicación con ARCA: " + mensaje(ex), ex);
        }
    }

    private synchronized Ticket ticket() throws Exception {
        Ticket current = cachedTicket;
        if (current != null && current.expiration().isAfter(Instant.now().plusSeconds(300))) return current;

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = properties.getKeystorePassword().toCharArray();
        try (var input = Files.newInputStream(Path.of(properties.getKeystorePath()))) {
            keyStore.load(input, password);
        }
        String alias = privateKeyAlias(keyStore);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
        certificate.checkValidity();

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
        String tra = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<loginTicketRequest version=\"1.0\"><header>"
                + "<uniqueId>" + Instant.now().getEpochSecond() + "</uniqueId>"
                + "<generationTime>" + now.minusMinutes(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "</generationTime>"
                + "<expirationTime>" + now.plusMinutes(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "</expirationTime>"
                + "</header><service>wsfe</service></loginTicketRequest>";

        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(privateKey);
        generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
        ).build(signer, certificate));
        generator.addCertificates(new JcaCertStore(Collections.singletonList(certificate)));
        CMSSignedData signed = generator.generate(
                new CMSProcessableByteArray(tra.getBytes(StandardCharsets.UTF_8)), true
        );

        String cms = Base64.getEncoder().encodeToString(signed.getEncoded());
        String body = "<loginCms xmlns=\"http://wsaa.view.sua.dvadac.desein.afip.gov\"><in0>"
                + cms + "</in0></loginCms>";
        Document response = sendSoap(properties.getWsaaUrl(), "", body);
        Document credentials = parse(requiredText(response, "loginCmsReturn"));
        Ticket loaded = new Ticket(
                requiredText(credentials, "token"),
                requiredText(credentials, "sign"),
                OffsetDateTime.parse(requiredText(credentials, "expirationTime")).toInstant()
        );
        cachedTicket = loaded;
        return loaded;
    }

    private long ultimoAutorizado(Ticket ticket, long cuit, int puntoVenta, int tipo) throws Exception {
        String body = "<FECompUltimoAutorizado xmlns=\"" + WSFE_NS + "\">"
                + auth(ticket, cuit) + "<PtoVta>" + puntoVenta + "</PtoVta><CbteTipo>" + tipo
                + "</CbteTipo></FECompUltimoAutorizado>";
        Document response = sendSoap(properties.getWsfeUrl(), WSFE_NS + "FECompUltimoAutorizado", body);
        String number = optionalText(response, "CbteNro");
        if (number == null) throw new ArcaException(describe(messages(response), "ARCA no devolvió el último comprobante autorizado"));
        return Long.parseLong(number);
    }

    private ResultadoArca solicitarCae(Ticket ticket, long cuit, FacturaVenta factura, Date fechaComprobante,
                                       int puntoVenta, int tipo, long numero) throws Exception {
        Cliente cliente = factura.getCliente();
        int docTipo = cliente == null ? 99 : cliente.getTipoDocumento().getCodigoAfip();
        long docNro = cliente == null ? 0 : Long.parseLong(digitos(cliente.getCuitCuil()));
        int condicionIva = factura.getCondicionIva().getCodigoAfip();
        String fecha = fechaComprobante.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate().format(ARCA_DATE);
        BigDecimal total = factura.getImporteTotal().setScale(2, RoundingMode.HALF_UP);
        String moneda = factura.getTipoMoneda().getCodigoAfip();

        String detail = "<FECAEDetRequest><Concepto>1</Concepto><DocTipo>" + docTipo
                + "</DocTipo><DocNro>" + docNro + "</DocNro><CbteDesde>" + numero
                + "</CbteDesde><CbteHasta>" + numero + "</CbteHasta><CbteFch>" + fecha
                + "</CbteFch><ImpTotal>" + total.toPlainString() + "</ImpTotal><ImpTotConc>0.00</ImpTotConc>"
                + "<ImpNeto>" + total.toPlainString() + "</ImpNeto><ImpOpEx>0.00</ImpOpEx>"
                + "<ImpTrib>0.00</ImpTrib><ImpIVA>0.00</ImpIVA><MonId>" + xml(moneda)
                + "</MonId><MonCotiz>1.000000</MonCotiz><CondicionIVAReceptorId>" + condicionIva
                + "</CondicionIVAReceptorId></FECAEDetRequest>";
        String body = "<FECAESolicitar xmlns=\"" + WSFE_NS + "\">" + auth(ticket, cuit)
                + "<FeCAEReq><FeCabReq><CantReg>1</CantReg><PtoVta>" + puntoVenta
                + "</PtoVta><CbteTipo>" + tipo + "</CbteTipo></FeCabReq><FeDetReq>"
                + detail + "</FeDetReq></FeCAEReq></FECAESolicitar>";

        Document response = sendSoap(properties.getWsfeUrl(), WSFE_NS + "FECAESolicitar", body);
        List<ArcaEmisionDTO.MensajeDTO> responseMessages = messages(response);
        String result = optionalText(response, "Resultado");
        String cae = optionalText(response, "CAE");
        String expiration = optionalText(response, "CAEFchVto");
        boolean approved = "A".equalsIgnoreCase(result) && cae != null && !cae.isBlank();
        Date expirationDate = expiration == null || expiration.isBlank() ? null
                : java.sql.Date.valueOf(java.time.LocalDate.parse(expiration, ARCA_DATE));
        String summary = approved ? "ARCA autorizó el comprobante"
                : describe(responseMessages, "ARCA rechazó el comprobante");
        return new ResultadoArca(approved, numero, result, cae, expirationDate, summary, responseMessages);
    }

    private String auth(Ticket ticket, long cuit) {
        return "<Auth><Token>" + xml(ticket.token()) + "</Token><Sign>" + xml(ticket.sign())
                + "</Sign><Cuit>" + cuit + "</Cuit></Auth>";
    }

    private Document sendSoap(String url, String action, String operation) throws Exception {
        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\""
                + SOAP_NS + "\"><soap:Header/><soap:Body>" + operation + "</soap:Body></soap:Envelope>";
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds())).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", action == null || action.isBlank() ? "\"\"" : "\"" + action + "\"")
                .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Document document = parse(response.body());
        String fault = optionalText(document, "faultstring");
        if (response.statusCode() >= 400 || fault != null) {
            throw new ArcaException(fault == null ? "ARCA respondió HTTP " + response.statusCode() : fault);
        }
        return document;
    }

    private Document parse(String content) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(content)));
    }

    private List<ArcaEmisionDTO.MensajeDTO> messages(Document document) {
        List<ArcaEmisionDTO.MensajeDTO> result = new ArrayList<>();
        collectMessages(document, "Err", "ERROR", result);
        collectMessages(document, "Obs", "OBSERVACION", result);
        collectMessages(document, "Evt", "EVENTO", result);
        return result;
    }

    private void collectMessages(Document document, String tag, String type,
                                 List<ArcaEmisionDTO.MensajeDTO> target) {
        NodeList nodes = document.getElementsByTagNameNS("*", tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) continue;
            target.add(new ArcaEmisionDTO.MensajeDTO(type, childText(element, "Code"), childText(element, "Msg")));
        }
    }

    private String childText(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagNameNS("*", name);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String requiredText(Document document, String localName) {
        String value = optionalText(document, localName);
        if (value == null || value.isBlank()) throw new ArcaException("La respuesta de ARCA no contiene " + localName);
        return value;
    }

    private String optionalText(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    private String privateKeyAlias(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) return alias;
        }
        throw new ArcaException("El PKCS#12 no contiene una clave privada");
    }

    private void validarConfiguracion() {
        if (properties.getCuit() == null || digitos(properties.getCuit()).length() != 11)
            throw new ArcaException("Falta configurar ARCA_CUIT con 11 dígitos");
        if (properties.getKeystorePath() == null || properties.getKeystorePath().isBlank())
            throw new ArcaException("Falta configurar ARCA_KEYSTORE_PATH");
        if (!Files.isRegularFile(Path.of(properties.getKeystorePath())))
            throw new ArcaException("No existe el certificado indicado en ARCA_KEYSTORE_PATH");
        if (properties.getKeystorePassword() == null || properties.getKeystorePassword().isBlank())
            throw new ArcaException("Falta configurar ARCA_KEYSTORE_PASSWORD");
    }

    private int tipoComprobante() {
        String condicion = properties.getCondicionIvaEmisor() == null
                ? "" : properties.getCondicionIvaEmisor().trim().toUpperCase();
        if (condicion.contains("MONOTRIBUTO") || condicion.contains("EXENTO")) return 11;
        throw new ArcaException("La integración actual está preparada para Factura C de monotributistas o exentos");
    }

    private String describe(List<ArcaEmisionDTO.MensajeDTO> values, String fallback) {
        return values.stream().map(ArcaEmisionDTO.MensajeDTO::descripcion)
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(fallback);
    }

    private String digitos(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    private boolean esEndpointHomologacion(String value, String hostEsperado) {
        if (value == null || value.isBlank()) return false;
        try {
            return hostEsperado.equalsIgnoreCase(URI.create(value).getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
    private String mensaje(Exception ex) { return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(); }
    private String xml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private record Ticket(String token, String sign, Instant expiration) { }

    public record ResultadoArca(boolean aprobada, long numero, String resultado, String cae,
                                Date vencimiento, String mensaje,
                                List<ArcaEmisionDTO.MensajeDTO> mensajes) { }

    public static class ArcaException extends RuntimeException {
        public ArcaException(String message) { super(message); }
        public ArcaException(String message, Throwable cause) { super(message, cause); }
    }
}
