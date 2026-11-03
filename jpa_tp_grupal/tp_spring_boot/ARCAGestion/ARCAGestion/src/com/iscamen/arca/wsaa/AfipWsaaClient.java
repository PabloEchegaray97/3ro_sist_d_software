package com.iscamen.arca.wsaa;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import com.iscamen.arca.util.AfipHttpUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;

public class AfipWsaaClient {

    private static final DateTimeFormatter AFIP_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static String[] obtenerTokenYSign(String keyPath, String certPath, String servicio, boolean isProduccion, String cuit) throws Exception {
        File cacheFile = new File("ticket-" + (isProduccion ? "prod-" : "homo-") + servicio + ".properties");

        if (cacheFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                props.load(fis);
            }

            long expMillis = Long.parseLong(props.getProperty("expiration", "0"));
            if (System.currentTimeMillis() < (expMillis - (10 * 60 * 1000))) {
                System.out.println("-> Usando Token y Sign desde archivo de caché local.");
                return new String[]{props.getProperty("token"), props.getProperty("sign")};
            }
        }

        System.out.println("-> Solicitando nuevo Token y Sign a WSAA...");
        return solicitarNuevoTicket(keyPath, certPath, servicio, cacheFile, isProduccion, cuit);
    }

    private static String[] solicitarNuevoTicket(String keyPath, String certPath, String servicio, File cacheFile, boolean isProduccion, String cuit) throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        PrivateKey privateKey = cargarClavePrivada(keyPath);
        X509Certificate cert = cargarCertificado(certPath);

        // Truncar nanosegundos para evitar milisegundos en el XML
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String genTime = now.minusMinutes(10).format(AFIP_DATE_FORMATTER);
        String expTime = now.plusMinutes(10).format(AFIP_DATE_FORMATTER);

        String destination = isProduccion 
                ? "cn=wsaa,o=afip,c=ar,serialNumber=CUIT " + cuit
                : "cn=wsaahomo,o=afip,c=ar,serialNumber=CUIT " + cuit ;

        String traXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<loginTicketRequest version=\"1.0\">" +
                "<header>" +
                "<destination>" + destination + "</destination>" +
                "<uniqueId>" + (System.currentTimeMillis() / 1000) + "</uniqueId>" +
                "<generationTime>" + genTime + "</generationTime>" +
                "<expirationTime>" + expTime + "</expirationTime>" +
                "</header>" +
                "<service>" + servicio + "</service>" +
                "</loginTicketRequest>";

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(privateKey);

        gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder()
                                .setProvider("BC")
                                .build())
                        .build(signer, cert)
        );

        gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));

        CMSProcessableByteArray msg = new CMSProcessableByteArray(traXml.getBytes(StandardCharsets.UTF_8));
        CMSSignedData signedData = gen.generate(msg, true);

        String cmsBase64 = Base64.getEncoder().encodeToString(signedData.getEncoded());

        String soapEnvelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:nso=\"http://wsaa.afip.gov.ar/ws/services/LoginCms\">" +
                "<soapenv:Header/>" +
                "<soapenv:Body>" +
                "<nso:loginCms><nso:in0>" + cmsBase64 + "</nso:in0></nso:loginCms>" +
                "</soapenv:Body></soapenv:Envelope>";

        String xmlResponse = enviarSoap(AfipHttpUtil.getURLWsaa(isProduccion), soapEnvelope, "");

        String xmlLimpio = xmlResponse.replace("&lt;", "<")
                                     .replace("&gt;", ">")
                                     .replace("&quot;", "\"")
                                     .replace("&amp;", "&");

        if (!xmlLimpio.contains("<token>")) {
            throw new RuntimeException("Error en la respuesta de WSAA: " + xmlResponse);
        }

        String token = xmlLimpio.substring(xmlLimpio.indexOf("<token>") + 7, xmlLimpio.indexOf("</token>"));
        String sign = xmlLimpio.substring(xmlLimpio.indexOf("<sign>") + 6, xmlLimpio.indexOf("</sign>"));
        String expirationStr = xmlLimpio.substring(xmlLimpio.indexOf("<expirationTime>") + 16, xmlLimpio.indexOf("</expirationTime>"));

        long expirationMillis = ZonedDateTime.parse(expirationStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli();

        Properties props = new Properties();
        props.setProperty("token", token);
        props.setProperty("sign", sign);
        props.setProperty("expiration", String.valueOf(expirationMillis));

        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            props.store(fos, "Ticket de Acceso AFIP - " + servicio);
        }

        return new String[]{token, sign};
    }

    private static PrivateKey cargarClavePrivada(String keyPath) throws Exception {
        try (FileReader fr = new FileReader(keyPath);
             PEMParser pemParser = new PEMParser(fr)) {
            Object parsedObject = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (parsedObject instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) parsedObject).getPrivate();
            } else if (parsedObject instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) parsedObject);
            } else if (parsedObject instanceof PrivateKey) {
                return (PrivateKey) parsedObject;
            } else {
                throw new IllegalArgumentException("Objeto de clave no reconocido en: " + keyPath);
            }
        }
    }

    private static X509Certificate cargarCertificado(String certPath) throws Exception {
        try (InputStream is = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    private static String enviarSoap(String endpointUrl, String soapXml, String soapAction) throws Exception {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        
        // CORRECCIÓN: Enviar siempre el header SOAPAction aunque sea una cadena vacía ""
        if (soapAction != null) {
            conn.setRequestProperty("SOAPAction", soapAction);
        }

        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = soapXml.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        InputStream is = (conn.getResponseCode() >= 400) ? conn.getErrorStream() : conn.getInputStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        return response.toString();
    }
}