package com.iscamen.arca.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.ws.BindingProvider;

import com.iscamen.arca.model.dto.FacturaRequest;

import ar.gov.afip.wsfe.AlicIva;
import ar.gov.afip.wsfe.ArrayOfAlicIva;
import ar.gov.afip.wsfe.ArrayOfFECAEDetRequest;
import ar.gov.afip.wsfe.Err;
import ar.gov.afip.wsfe.FEAuthRequest;
import ar.gov.afip.wsfe.FECAECabRequest;
import ar.gov.afip.wsfe.FECAEDetRequest;
import ar.gov.afip.wsfe.FECAEDetResponse;
import ar.gov.afip.wsfe.FECAERequest;
import ar.gov.afip.wsfe.FECAEResponse;
import ar.gov.afip.wsfe.Obs;
import ar.gov.afip.wsfe.Service;
import ar.gov.afip.wsfe.ServiceSoap;

public class AfipFacturaService {

    public static FECAEResponse emitirComprobante(String token, String sign, long cuitEmisor, int cbteNro, FacturaRequest req) {
    	Service service = new Service();
        ServiceSoap port = service.getServiceSoap();
        
        // FORZAR ENDPOINT DE PRODUCCIÓN DE WSFE
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(
            BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
            "https://servicios1.afip.gov.ar/wsfev1/service.asmx"
        );

        // 1. Autenticación
        FEAuthRequest auth = new FEAuthRequest();
        auth.setToken(token.trim());
        auth.setSign(sign.trim());
        auth.setCuit(cuitEmisor);

        System.out.println("Enviando factura Pto de Venta..." + req.getPtoVta());
        System.out.println("Enviando factura Tipo Cte..." + req.getCbteTipo());
        // 2. Cabecera
        FECAECabRequest cab = new FECAECabRequest();
        cab.setCantReg(1);
        cab.setPtoVta(req.getPtoVta());
        cab.setCbteTipo(req.getCbteTipo());

        // 3. Detalle genérico
        FECAEDetRequest det = new FECAEDetRequest();
        det.setConcepto(req.getConcepto());
        det.setDocTipo(req.getDocTipo());
        det.setDocNro(req.getDocNro());
        det.setCbteDesde(cbteNro);
        det.setCbteHasta(cbteNro);
        
        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        det.setCbteFch(fechaHoy);

        // Fechas requeridas si se prestan Servicios (Concepto 2 o 3)
        if (req.getConcepto() == 2 || req.getConcepto() == 3) {
            det.setFchServDesde(req.getFchServDesde() != null ? req.getFchServDesde() : fechaHoy);
            det.setFchServHasta(req.getFchServHasta() != null ? req.getFchServHasta() : fechaHoy);
            det.setFchVtoPago(req.getFchVtoPago() != null ? req.getFchVtoPago() : fechaHoy);
        }

        det.setMonId("PES");
        det.setMonCotiz(1.0);
        det.setCondicionIVAReceptorId(req.getCondicionIvaReceptor());

        // 4. Lógica de Desglose de IVA según el Tipo de Comprobante
        double impTotal = req.getImporteTotal();

        if (req.getCbteTipo() == 11) {
            // FACTURA C (Monotributo / Exento): Sin IVA desglosado
            det.setImpTotal(impTotal);
            det.setImpTotConc(0.00);
            det.setImpNeto(impTotal);
            det.setImpOpEx(0.00);
            det.setImpTrib(0.00);
            det.setImpIVA(0.00);
        } else {
            // FACTURA A o B (Responsable Inscripto): Requiere cálculo y nodo <Iva>
            double alicuota = req.getAlicuotaIvaPct();
            double impNeto = Math.round((impTotal / (1.0 + (alicuota / 100.0))) * 100.0) / 100.0;
            double impIva = Math.round((impTotal - impNeto) * 100.0) / 100.0;

            det.setImpTotal(impTotal);
            det.setImpTotConc(0.00);
            det.setImpNeto(impNeto);
            det.setImpOpEx(0.00);
            det.setImpTrib(0.00);
            det.setImpIVA(impIva);

            // Mapeo del ID de alícuota AFIP (5 = 21%, 4 = 10.5%, 6 = 27%)
            int idAlicuotaAfip = obtenerIdAlicuotaAfip(alicuota);

            AlicIva alicIva = new AlicIva();
            alicIva.setId(idAlicuotaAfip);
            alicIva.setBaseImp(impNeto);
            alicIva.setImporte(impIva);

            ArrayOfAlicIva arrayIva = new ArrayOfAlicIva();
            arrayIva.getAlicIva().add(alicIva);
            det.setIva(arrayIva);
        }

        ArrayOfFECAEDetRequest detArray = new ArrayOfFECAEDetRequest();
        detArray.getFECAEDetRequest().add(det);

        FECAERequest feCAEReq = new FECAERequest();
        feCAEReq.setFeCabReq(cab);
        feCAEReq.setFeDetReq(detArray);

        FECAEResponse response = port.fecaeSolicitar(auth, feCAEReq);
        if (response == null) {
            throw new RuntimeException("La respuesta de AFIP/ARCA fue nula.");
        }

        // 1. Validar si hay errores a nivel general/cabecera
        if (response.getErrors() != null && response.getErrors().getErr() != null) {
            for (Err err : response.getErrors().getErr()) {
                System.err.println("Error AFIP " + err.getCode() + ": " + err.getMsg());
            }
            throw new RuntimeException("AFIP devolvió errores en la cabecera.");
        }

        // 2. Validar que exista la respuesta de detalle
        if (response.getFeDetResp() == null || response.getFeDetResp().getFECAEDetResponse() == null 
                || response.getFeDetResp().getFECAEDetResponse().isEmpty()) {
            throw new RuntimeException("AFIP no devolvió detalles de comprobantes en la respuesta.");
        }

        FECAEDetResponse detResp = response.getFeDetResp().getFECAEDetResponse().get(0); // o [0] si es array

        // 3. Evaluar el resultado (A = Aprobado, R = Rechazado, P = Parcial)
        String resultado = detResp.getResultado();
        System.out.println("Resultado de la operación: " + resultado);

        if ("A".equals(resultado)) {
            String cae = detResp.getCAE();
            String fchVto = detResp.getCAEFchVto();
            System.out.println("¡Comprobante autorizado!");
            System.out.println("CAE: " + cae);
            System.out.println("Vencimiento CAE: " + fchVto);
        } else {
            System.err.println("Comprobante RECHAZADO.");
            // Imprimir las observaciones del rechazo
            if (detResp.getObservaciones() != null && detResp.getObservaciones().getObs() != null) {
                for (Obs obs : detResp.getObservaciones().getObs()) {
                    System.err.println("Observación " + obs.getCode() + ": " + obs.getMsg());
                }
            }
        }
        
        return response;
    }

    private static int obtenerIdAlicuotaAfip(double alicuotaPct) {
        if (alicuotaPct == 21.0) return 5;
        if (alicuotaPct == 10.5) return 4;
        if (alicuotaPct == 27.0) return 6;
        if (alicuotaPct == 5.0)  return 8;
        if (alicuotaPct == 2.5)  return 9;
        return 5; // Por defecto 21%
    }
}