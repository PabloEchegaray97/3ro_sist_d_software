package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "arca.homologacion")
public class ArcaHomologacionProperties {
    private String cuit;
    private String condicionIvaEmisor = "MONOTRIBUTO";
    private String razonSocialEmisor = "Test Facturas";
    private String ingresosBrutosEmisor = "No contribuyente (TEST)";
    private String telefonoEmisor = "2615000000";
    private String logoPath = "";
    private String keystorePath;
    private String keystorePassword;
    private String wsaaUrl;
    private String wsfeUrl;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 30;

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }
    public String getCondicionIvaEmisor() { return condicionIvaEmisor; }
    public void setCondicionIvaEmisor(String condicionIvaEmisor) { this.condicionIvaEmisor = condicionIvaEmisor; }
    public String getRazonSocialEmisor() { return razonSocialEmisor; }
    public void setRazonSocialEmisor(String razonSocialEmisor) { this.razonSocialEmisor = razonSocialEmisor; }
    public String getIngresosBrutosEmisor() { return ingresosBrutosEmisor; }
    public void setIngresosBrutosEmisor(String ingresosBrutosEmisor) { this.ingresosBrutosEmisor = ingresosBrutosEmisor; }
    public String getTelefonoEmisor() { return telefonoEmisor; }
    public void setTelefonoEmisor(String telefonoEmisor) { this.telefonoEmisor = telefonoEmisor; }
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public String getKeystorePath() { return keystorePath; }
    public void setKeystorePath(String keystorePath) { this.keystorePath = keystorePath; }
    public String getKeystorePassword() { return keystorePassword; }
    public void setKeystorePassword(String keystorePassword) { this.keystorePassword = keystorePassword; }
    public String getWsaaUrl() { return wsaaUrl; }
    public void setWsaaUrl(String wsaaUrl) { this.wsaaUrl = wsaaUrl; }
    public String getWsfeUrl() { return wsfeUrl; }
    public void setWsfeUrl(String wsfeUrl) { this.wsfeUrl = wsfeUrl; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
}
