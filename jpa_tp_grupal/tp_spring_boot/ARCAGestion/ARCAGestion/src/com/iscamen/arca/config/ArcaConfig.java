package com.iscamen.arca.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ArcaConfig {

    private static final Properties properties = new Properties();

    static {

        try (InputStream in = ArcaConfig.class
                .getClassLoader()
                .getResourceAsStream("resources/arca.properties")) {

            if (in == null) {
                throw new RuntimeException("No se encontró arca.properties");
            }

            properties.load(in);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String getCuit() {
        return get("cuit");
    }

    public static String getService() {
        return get("service");
    }

    public static String getKeystore() {
        return get("keystore");
    }

    public static String getPassword() {
        return get("keystore.password");
    }

    public static String getWsaaUrl() {
        return get("wsaa.url");
    }

    public static String getWsfeUrl() {
        return get("wsfe.url");
    }

}