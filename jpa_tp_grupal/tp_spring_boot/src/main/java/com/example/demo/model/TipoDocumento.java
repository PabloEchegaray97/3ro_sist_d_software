package com.example.demo.model;

public enum TipoDocumento {
    CUIT(80),
    CUIL(86);

    private final int codigoAfip;

    TipoDocumento(int codigoAfip) {
        this.codigoAfip = codigoAfip;
    }

    public int getCodigoAfip() {
        return codigoAfip;
    }
}
