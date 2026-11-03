package com.iscamen.arca;

import com.iscamen.arca.model.dto.FacturaRequest;
import com.iscamen.arca.service.AfipFacturaService;
import com.iscamen.arca.wsaa.AfipWsaaClient;
import com.iscamen.arca.wsfe.AfipWsfeClient;

import ar.gov.afip.wsfe.FECAEDetResponse;
import ar.gov.afip.wsfe.FECAEResponse;

public class Main {

	public static void main(String[] args) {
		try {
			long cuit = 20110000008L;          // Tu CUIT sin guiones
            int puntoDeVenta = 1;               //RECE  punto de venta configurado en ARCA/AFIP
            /*
            1: IVA Responsable Inscripto
			4: Sujeto Exento
			5: Consumidor Final
			6: Responsable Monotributo
             */
            int condicionIvaConsumidorFinal = 5;
            int tipoComprobante = 11;// Factura C / Nota de Débito C: cbteTipo = 12 / Nota de Crédito C: cbteTipo = 13 / Recibo C: cbteTipo = 15
            int docTipo = 99;//99 Sin identificar 80: CUIT, 96: DNI, 86: CUIL
            long docNro = 0L;
            //concepto: Cambia de 1 (Productos) a 2 (Servicios) o 3 (Productos y Servicios).
            int concepto = 1;
            boolean IS_PRODUCCION = false;
            
            FacturaRequest req = new FacturaRequest();
            req.setPtoVta(puntoDeVenta);
            req.setCbteTipo(tipoComprobante);               // Factura C
            req.setConcepto(concepto);               
            req.setDocTipo(docTipo);                
            req.setDocNro(docNro);
            req.setCondicionIvaReceptor(condicionIvaConsumidorFinal);    // Consumidor Final
            req.setImporteTotal(1500.00);
            if(concepto == 2) {//si es un servicio
            	// Fechas obligatorias de servicio (Formato: yyyyMMdd)
            	req.setFchServDesde("20260801");   // Inicio del período prestado
            	req.setFchServHasta("20260831");   // Fin del período prestado
            	req.setFchVtoPago("20260815");     // Vencimiento para la cancelación del pago
            }
            
            System.out.println("1. Solicitando Token y Sign a WSAA...");
            String keyPath = "D:\\ARCA-AFIP\\test\\clave_privada.key";
            String certPath = "D:\\ARCA-AFIP\\test\\certificado.crt";

            String[] auth = AfipWsaaClient.obtenerTokenYSign(keyPath, certPath, "wsfe", IS_PRODUCCION, String.valueOf(cuit));
            String token = auth[0];
            String sign = auth[1];
            System.out.println("Autenticado OK. Token activo.");

            System.out.println("2. Consultando último comprobante...");
            int ultimoNro = AfipWsfeClient.obtenerUltimoComprobante(token, sign, cuit, puntoDeVenta, tipoComprobante, IS_PRODUCCION);
            int nuevoNro = ultimoNro + 1;
            System.out.println("Último número registrado: " + ultimoNro + ". Siguiente a emitir: " + nuevoNro);

            System.out.println("3. Enviando factura para CAE...");
            
            //FECAEResponse xmlResponse = AfipWsfeClient.solicitarCae(token, sign, cuit, puntoDeVenta, nuevoNro, 1500.00, condicionIvaConsumidorFinal);
            
            FECAEResponse xmlResponse = AfipFacturaService.emitirComprobante(token, sign, cuit, nuevoNro, req);
            
            System.err.println("xmlResponse");
            System.out.println(xmlResponse.toString());
            // 2. Lectura directa de la cabecera ("A" Aprobado, "R" Rechazado)
            String resultadoCabecera = xmlResponse.getFeCabResp().getResultado();

            if ("A".equals(resultadoCabecera)) {
                FECAEDetResponse detResp = xmlResponse.getFeDetResp().getFECAEDetResponse().get(0);
                System.out.println("CAE asignado: " + detResp.getCAE());
                System.out.println("Vencimiento CAE: " + detResp.getCAEFchVto());
            } else {
                System.err.println("Comprobante rechazado o con observaciones.");
                
                // Observaciones específicas de cada comprobante enviado
                if (xmlResponse.getFeDetResp() != null && !xmlResponse.getFeDetResp().getFECAEDetResponse().isEmpty()) {
                    FECAEDetResponse detResp = xmlResponse.getFeDetResp().getFECAEDetResponse().get(0);
                    if (detResp.getObservaciones() != null) {
                        detResp.getObservaciones().getObs().forEach(obs -> 
                            System.err.println("Obs [" + obs.getCode() + "]: " + obs.getMsg())
                        );
                    }
                }
                
                // Errores globales del requerimiento (Auth, formato general, CUIT)
                if (xmlResponse.getErrors() != null) {
                	xmlResponse.getErrors().getErr().forEach(err -> 
                        System.err.println("Error [" + err.getCode() + "]: " + err.getMsg())
                    );
                }
            }
            
		} catch (Exception e) {

            e.printStackTrace();

        }
	}
	

}
