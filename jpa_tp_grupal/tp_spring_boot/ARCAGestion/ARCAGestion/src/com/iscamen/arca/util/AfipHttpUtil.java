package com.iscamen.arca.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AfipHttpUtil {
	
	private static final String WSAA_URL_TEST = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
	private static final String WSAA_URL_PROD = "https://wsaa.afip.gov.ar/ws/services/LoginCms";
	private static final String WSFE_URL_TEST = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
	private static final String WSFE_URL_PROD = "https://servicios1.afip.gov.ar/wsfev1/service.asmx";

	public static String getURLWsaa(boolean isProduccion) {
		if(isProduccion) {
			return WSAA_URL_PROD;
		}else {
			return WSAA_URL_TEST;
		}
	}
	
	public static String getURLWsfe(boolean isProduccion) {
		if(isProduccion) {
			return WSFE_URL_PROD;
		}else {
			return WSFE_URL_TEST;
		}
	}
	
	public static String enviarSoap(String endpointUrl, String soapXml, String soapAction) throws Exception {
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
