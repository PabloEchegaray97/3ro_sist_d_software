package com.iscamen.arca.model;

import java.util.ArrayList;
import java.util.List;

public class RespuestaCae {
    private String resultado;          // A = Aprobado, R = Rechazado, P = Parcial
    private String cae;                // Código de Autorización Electrónico
    private String caeFechaVencimiento; // Formato YYYYMMDD
    private List<String> observaciones = new ArrayList<>();
    private List<String> errores = new ArrayList<>();

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public String getCaeFechaVencimiento() { return caeFechaVencimiento; }
    public void setCaeFechaVencimiento(String caeFechaVencimiento) { this.caeFechaVencimiento = caeFechaVencimiento; }

    public List<String> getObservaciones() { return observaciones; }
    public List<String> getErrores() { return errores; }

    public boolean isAprobado() {
        return "A".equalsIgnoreCase(this.resultado);
    }

    @Override
    public String toString() {
        return "RespuestaCae {" +
                "\n  Resultado = '" + resultado + '\'' +
                ",\n  CAE = '" + cae + '\'' +
                ",\n  Vencimiento = '" + caeFechaVencimiento + '\'' +
                ",\n  Observaciones = " + observaciones +
                ",\n  Errores = " + errores +
                "\n}";
    }
}