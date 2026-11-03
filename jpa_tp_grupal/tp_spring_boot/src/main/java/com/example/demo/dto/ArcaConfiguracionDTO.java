package com.example.demo.dto;

public record ArcaConfiguracionDTO(
        String ambiente,
        String cuit,
        String condicionIvaEmisor,
        String razonSocialEmisor,
        String ingresosBrutosEmisor,
        String inicioActividadesEmisor,
        String telefonoEmisor,
        String logoPath,
        int tipoComprobanteCodigo,
        String tipoComprobanteDescripcion,
        boolean cuitConfigurado,
        String keystorePath,
        boolean keystoreConfigurado,
        boolean keystoreExiste,
        boolean passwordConfigurada,
        String wsaaUrl,
        String wsfeUrl,
        boolean endpointsHomologacion,
        boolean listaParaProbar
) {
}
