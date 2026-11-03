package com.iscamen.arca.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class CertificateManager {

    private KeyStore keyStore;
    private String alias;

    public CertificateManager(String path, String password) throws Exception {

        keyStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fis = new FileInputStream(path)) {
            keyStore.load(fis, password.toCharArray());
        }

        buscarAlias();
    }


    private void buscarAlias() throws Exception {

        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {

            String currentAlias = aliases.nextElement();

            if (keyStore.isKeyEntry(currentAlias)) {
                this.alias = currentAlias;
                return;
            }
        }

        throw new Exception("No se encontró una clave privada dentro del PKCS12");
    }


    public PrivateKey getPrivateKey(String password) throws Exception {

        return (PrivateKey) keyStore.getKey(
                alias,
                password.toCharArray()
        );
    }


    public X509Certificate getCertificate() throws Exception {

        return (X509Certificate) keyStore.getCertificate(alias);
    }


    public String getAlias() {
        return alias;
    }
}