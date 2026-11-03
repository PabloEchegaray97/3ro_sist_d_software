package com.iscamen.arca.auth;


import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;


public class CMSBuilder {


    static {

        Security.addProvider(
            new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );

    }



    public static String generarCMS(
            String xml,
            PrivateKey privateKey,
            X509Certificate certificado
    ) throws Exception {

    	
        ContentSigner signer =
            new JcaContentSignerBuilder(
                    "SHA1withRSA"
            )
            .setProvider("BC")
            .build(privateKey);



        CMSSignedDataGenerator generator =
                new CMSSignedDataGenerator();


        generator.addSignerInfoGenerator(
            new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder()
                    .setProvider("BC")
                    .build()
            )
            .build(
                signer,
                certificado
            )
        );

/*
        generator.addCertificates(
            new JcaCertStore(
                java.util.Collections.singletonList(certificado)
            )
        );
*/

        System.out.println("================================");
    	System.out.println(certificado.getSubjectDN());

    	List<X509Certificate> certs = new ArrayList<>();
    	certs.add(certificado);

    	System.out.println("Cantidad certificados: " + certs.size());

    	Store<?> certStore = new JcaCertStore(certs);

    	generator.addCertificates(certStore);

    	System.out.println("Certificados agregados al CMS");
    	System.out.println("================================");
    	
        CMSSignedData signedData =
            generator.generate(
                new CMSProcessableByteArray(
                    xml.getBytes("UTF-8")
                ),
                true
            );

        System.out.println("Version CMS: " + signedData.getVersion());
        System.out.println("Certificados CMS: "
                + signedData.getCertificates().getMatches(null).size());
        
        byte[] encoded =
                signedData.getEncoded();
        
        byte[] cms = signedData.getEncoded();

        Files.write(
            Paths.get("D:/ARCA-AFIP/cms.bin"),
            cms
        );


        return Base64.getEncoder()
                .encodeToString(cms);

    }

}