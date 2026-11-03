package com.iscamen.arca.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.iscamen.arca.model.RespuestaCae;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class AfipXmlParser {

    public static RespuestaCae parsearRespuestaCae(String xmlSoapResponse) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false); // Ignora prefijos de SOAP para simplificar la lectura
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        Document doc = builder.parse(new ByteArrayInputStream(xmlSoapResponse.getBytes(StandardCharsets.UTF_8)));
        RespuestaCae respuesta = new RespuestaCae();

        // 1. Extraer Resultado General
        NodeList resList = doc.getElementsByTagName("Resultado");
        if (resList.getLength() > 0) {
            respuesta.setResultado(resList.item(0).getTextContent());
        }

        // 2. Extraer CAE
        NodeList caeList = doc.getElementsByTagName("CAE");
        if (caeList.getLength() > 0) {
            respuesta.setCae(caeList.item(0).getTextContent());
        }

        // 3. Extraer Fecha de Vencimiento del CAE
        NodeList vtoList = doc.getElementsByTagName("CAEFchVto");
        if (vtoList.getLength() > 0) {
            respuesta.setCaeFechaVencimiento(vtoList.item(0).getTextContent());
        }

        // 4. Extraer Observaciones (Si el comprobante se aprobó pero con advertencias)
        NodeList obsList = doc.getElementsByTagName("Obs");
        for (int i = 0; i < obsList.getLength(); i++) {
            Element obsElem = (Element) obsList.item(i);
            String code = getTagValue("Code", obsElem);
            String msg = getTagValue("Msg", obsElem);
            respuesta.getObservaciones().add("[" + code + "] " + msg);
        }

        // 5. Extraer Errores Globales (Si la solicitud fue rechazada)
        NodeList errList = doc.getElementsByTagName("Err");
        for (int i = 0; i < errList.getLength(); i++) {
            Element errElem = (Element) errList.item(i);
            String code = getTagValue("Code", errElem);
            String msg = getTagValue("Msg", errElem);
            respuesta.getErrores().add("[" + code + "] " + msg);
        }

        return respuesta;
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nl = element.getElementsByTagName(tag);
        if (nl != null && nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        return "";
    }
}