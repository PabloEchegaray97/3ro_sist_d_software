package com.iscamen.arca.wsfe;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.iscamen.arca.util.AfipHttpUtil;

import ar.gov.afip.wsfe.ArrayOfFECAEDetRequest;
import ar.gov.afip.wsfe.FEAuthRequest;
import ar.gov.afip.wsfe.FECAECabRequest;
import ar.gov.afip.wsfe.FECAEDetRequest;
import ar.gov.afip.wsfe.FECAERequest;
import ar.gov.afip.wsfe.FECAEResponse;
import ar.gov.afip.wsfe.Service;
import ar.gov.afip.wsfe.ServiceSoap;

public class AfipWsfeClient {

    
    public static int obtenerUltimoComprobante(String token, String sign, long cuit, int ptoVta, int cbteTipo, boolean isProduccion) throws Exception {
        String soapXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "  <FECompUltimoAutorizado xmlns=\"http://ar.gov.afip.dif.FEV1/\">" +
                "<Auth><Token>" + token + "</Token><Sign>" + sign + "</Sign><Cuit>" + cuit + "</Cuit></Auth>" +
                "<PtoVta>" + ptoVta + "</PtoVta>" +
                "<CbteTipo>" + cbteTipo + "</CbteTipo>" +
                "  </FECompUltimoAutorizado>" +
                "</soap:Body></soap:Envelope>";

        String response = enviarSoap(AfipHttpUtil.getURLWsfe(isProduccion), soapXml, "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado");
        String nro = response.substring(response.indexOf("<CbteNro>") + 9, response.indexOf("</CbteNro>"));
        return Integer.parseInt(nro);
    }

    public static FECAEResponse solicitarCae(String token, String sign, long cuit, int ptoVta, int cbteNro, double importeTotal, int condicionIvaReceptor) throws Exception {
    	// 1. Inicializar el cliente SOAP autogenerado
        Service service = new Service();
        ServiceSoap port = service.getServiceSoap();

        // 2. Encabezado de Autenticación (Auth)
        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(token.trim());
        auth.setSign(sign.trim());
        auth.setCuit(cuit);

        // 3. Estructura principal del requerimiento
        FECAERequest req = new FECAERequest();

        // Cabecera del comprobante (Factura C = 11, Factura B = 6)
        FECAECabRequest cab = new FECAECabRequest();
        cab.setCantReg(1);
        cab.setPtoVta(ptoVta);
        cab.setCbteTipo(11); // Ejemplo para Factura C
        req.setFeCabReq(cab);

        // Detalle del comprobante
        FECAEDetRequest det = new FECAEDetRequest();
        det.setConcepto(1); // 1 = Productos
        det.setDocTipo(99); // 99 = Sin identificar / Consumidor Final
        det.setDocNro(0);
        det.setCbteDesde(cbteNro);
        det.setCbteHasta(cbteNro);
        det.setCbteFch(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        det.setImpTotal(importeTotal);
        det.setImpTotConc(0.00);
        det.setImpNeto(importeTotal);
        det.setImpOpEx(0.00);
        det.setImpTrib(0.00);
        det.setImpIVA(0.00);
        det.setMonId("PES");
        det.setMonCotiz(1.0);

        // Campo RG 5616 - JAXB lo ordenará exactamente en la posición requerida por el XSD
        det.setCondicionIVAReceptorId(5); // 5 = Consumidor Final

        // Agregar detalle al arreglo de detalles
        ArrayOfFECAEDetRequest detArray = new ArrayOfFECAEDetRequest();
        detArray.getFECAEDetRequest().add(det);
        req.setFeDetReq(detArray);

        // 4. Invocación SOAP síncrona
        // 1. Invocación asignando directamente a FECAEResponse
        FECAEResponse response = port.fecaeSolicitar(auth, req);
        return response;
       
    }

    private static String enviarSoap(String endpointUrl, String soapXml, String soapAction) throws Exception {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");

        if (soapAction != null && !soapAction.isEmpty()) {
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