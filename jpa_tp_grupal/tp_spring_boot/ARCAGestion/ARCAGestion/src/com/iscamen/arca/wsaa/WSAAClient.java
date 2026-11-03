package com.iscamen.arca.wsaa;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.soap.*;

import com.iscamen.arca.config.ArcaConfig;

public class WSAAClient {


    public String loginCms(String cms) throws Exception {


    	String endpoint = ArcaConfig.getWsaaUrl();

        System.out.println("Conectando a: " + endpoint);



        SOAPConnectionFactory soapConnectionFactory =
                SOAPConnectionFactory.newInstance();


        SOAPConnection connection =
                soapConnectionFactory.createConnection();


        SOAPMessage request =
                createRequest(cms);



        MimeHeaders headers =
                request.getMimeHeaders();

        headers.addHeader(
                "SOAPAction",
                "loginCms"
        );

        request.saveChanges();
        
        request.writeTo(System.out);
        System.out.println();


        SOAPMessage response =
                connection.call(
                        request,
                        new URL(endpoint)
                );


        connection.close();


        return extractResponse(response);

    }


    private SOAPMessage createRequest(String cms)
            throws Exception {


        MessageFactory messageFactory =
                MessageFactory.newInstance();


        SOAPMessage message =
                messageFactory.createMessage();


        SOAPEnvelope envelope =
                message.getSOAPPart().getEnvelope();


        SOAPBody body =
                envelope.getBody();


        QName bodyName =
                new QName(
                    "http://wsaa.view.sua.dvadac.desein.afip.gov/",
                    "loginCms",
                    "ns1"
                );


        SOAPElement loginCms =
                body.addChildElement(bodyName);


        SOAPElement in0 =
                loginCms.addChildElement("in0");


        in0.addTextNode(cms);


        message.getMimeHeaders().addHeader(
            "Content-Type",
            "text/xml; charset=utf-8"
        );


        message.saveChanges();


        return message;
    }


    private String extractResponse(
            SOAPMessage response)
            throws Exception {


        ByteArrayOutputStream out =
                new ByteArrayOutputStream();


        response.writeTo(out);


        return out.toString("UTF-8");

    }

}