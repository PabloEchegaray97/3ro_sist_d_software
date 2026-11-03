package com.iscamen.arca.model.dto;

public class FacturaRequest {
    private int ptoVta;
    private int cbteTipo;             // 1 = Factura A, 6 = Factura B, 11 = Factura C
    private int concepto;             // 1 = Productos, 2 = Servicios, 3 = Productos y Servicios
    private int docTipo;              // 80 = CUIT, 96 = DNI, 99 = Sin Identificar
    private long docNro;              // CUIT, DNI o 0 para Consumidor Final
    private int condicionIvaReceptor; // 1 = RI, 4 = Exento, 5 = Consumidor Final, 6 = Monotributo
    private double importeTotal;
    private double alicuotaIvaPct;    // 21.0, 10.5, 0.0 (Para Factura A y B)

    // Fechas obligatorias solo si concepto == 2 o 3 (Servicios)
    private String fchServDesde;      // yyyyMMdd
    private String fchServHasta;      // yyyyMMdd
    private String fchVtoPago;        // yyyyMMdd

    // Getters y Setters
    public int getPtoVta() { return ptoVta; }
    public void setPtoVta(int ptoVta) { this.ptoVta = ptoVta; }
    public int getCbteTipo() { return cbteTipo; }
    public void setCbteTipo(int cbteTipo) { this.cbteTipo = cbteTipo; }
    public int getConcepto() { return concepto; }
    public void setConcepto(int concepto) { this.concepto = concepto; }
    public int getDocTipo() { return docTipo; }
    public void setDocTipo(int docTipo) { this.docTipo = docTipo; }
    public long getDocNro() { return docNro; }
    public void setDocNro(long docNro) { this.docNro = docNro; }
    public int getCondicionIvaReceptor() { return condicionIvaReceptor; }
    public void setCondicionIvaReceptor(int condicionIvaReceptor) { this.condicionIvaReceptor = condicionIvaReceptor; }
    public double getImporteTotal() { return importeTotal; }
    public void setImporteTotal(double importeTotal) { this.importeTotal = importeTotal; }
    public double getAlicuotaIvaPct() { return alicuotaIvaPct; }
    public void setAlicuotaIvaPct(double alicuotaIvaPct) { this.alicuotaIvaPct = alicuotaIvaPct; }
    public String getFchServDesde() { return fchServDesde; }
    public void setFchServDesde(String fchServDesde) { this.fchServDesde = fchServDesde; }
    public String getFchServHasta() { return fchServHasta; }
    public void setFchServHasta(String fchServHasta) { this.fchServHasta = fchServHasta; }
    public String getFchVtoPago() { return fchVtoPago; }
    public void setFchVtoPago(String fchVtoPago) { this.fchVtoPago = fchVtoPago; }
}