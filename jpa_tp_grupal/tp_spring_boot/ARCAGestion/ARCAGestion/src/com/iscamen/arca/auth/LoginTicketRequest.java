package com.iscamen.arca.auth;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;


public class LoginTicketRequest {


    public static String generarXML(String servicio) {


        long uniqueId = System.currentTimeMillis();


        SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date ahora = new Date();

        String generationTime =
                sdf.format(
                    new Date(ahora.getTime() - 120000)
                );

        String expirationTime =
                sdf.format(
                    new Date(ahora.getTime() + 1200000)
                );

        return
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        +
        "<loginTicketRequest version=\"1.0\">"
        +
            "<header>"
            +
                "<uniqueId>"
                + uniqueId +
                "</uniqueId>"
            +
                "<generationTime>"
                + generationTime
                + "</generationTime>"
            +
                "<expirationTime>"
                + expirationTime
                + "</expirationTime>"
            +
            "</header>"
            +
            "<service>"
            + servicio
            + "</service>"
        +
        "</loginTicketRequest>";

    }

}