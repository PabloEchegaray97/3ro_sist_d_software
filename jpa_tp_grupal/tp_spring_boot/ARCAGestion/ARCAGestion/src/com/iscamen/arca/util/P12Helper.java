package com.iscamen.arca.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class P12Helper {
	
	public static Object[] obtenerClaveYCertificado(String p12Path, String p12Password) throws Exception {
        // Load the PKCS12 KeyStore
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12Path)) {
            ks.load(fis, p12Password.toCharArray());
        }

        // Get the inner alias (usually unique inside the .p12)
        String alias = ks.aliases().nextElement();

        // Extract Private Key and Certificate
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, p12Password.toCharArray());
        X509Certificate certificate = (X509Certificate) ks.getCertificate(alias);

        return new Object[] { privateKey, certificate };
    }
	
}