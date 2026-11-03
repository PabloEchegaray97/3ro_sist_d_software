import { useEffect, useState } from "react";
import { sileo } from "sileo";
import type { ArcaConfiguracionDTO, SistemaInfoDTO } from "./models/factura.dto";
import { errorMessage } from "./formatters";
import { Modal } from "./Modal";
import { FacturaService } from "./services/FacturaService";

export function SystemInfoModal({ open, service, onClose }: { open: boolean; service: FacturaService; onClose: () => void }) {
  const [arca, setArca] = useState<ArcaConfiguracionDTO | null>(null);
  const [info, setInfo] = useState<SistemaInfoDTO | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    let active = true;
    setLoading(true);
    Promise.all([service.obtenerConfiguracionArca(), service.obtenerInfoSistema()])
      .then(([arcaResult, infoResult]) => {
        if (!active) return;
        setArca(arcaResult);
        setInfo(infoResult);
      })
      .catch((error) => { if (active) sileo.error({ title: "No se pudo cargar la información", description: errorMessage(error) }); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [open, service]);

  return <Modal open={open} title="Información del sistema" className="wide-dialog system-info-dialog" onClose={onClose}>
    {loading && <p className="system-info-loading">Cargando configuración…</p>}
    {!loading && arca && info && <div className="system-info-content">
      <section>
        <h3>ARCA · Homologación</h3>
        <dl className="system-info-grid">
          <div><dt>Ambiente</dt><dd>{arca.ambiente}</dd></div>
          <div><dt>CUIT emisor</dt><dd>{arca.cuit ?? "Sin configurar"}</dd></div>
          <div><dt>Condición fiscal</dt><dd>{arca.condicionIvaEmisor}</dd></div>
          <div><dt>Razón social</dt><dd>{arca.razonSocialEmisor}</dd></div>
          <div><dt>Ingresos Brutos</dt><dd>{arca.ingresosBrutosEmisor}</dd></div>
          <div><dt>Inicio de actividades</dt><dd>{arca.inicioActividadesEmisor}</dd></div>
          <div><dt>Teléfono</dt><dd>{arca.telefonoEmisor}</dd></div>
          <div><dt>Logo</dt><dd>{arca.logoPath || "Espacio reservado"}</dd></div>
          <div><dt>Comprobante</dt><dd>{arca.tipoComprobanteDescripcion} · Código {arca.tipoComprobanteCodigo}</dd></div>
          <div><dt>Certificado PKCS#12</dt><dd data-ready={arca.keystoreExiste}>{arca.keystoreExiste ? "Configurado" : "Pendiente"}</dd></div>
          <div><dt>Contraseña del certificado</dt><dd data-ready={arca.passwordConfigurada}>{arca.passwordConfigurada ? "Configurada" : "Pendiente"}</dd></div>
          <div className="system-info-wide"><dt>Ruta del certificado</dt><dd>{arca.keystorePath ?? "Sin configurar"}</dd></div>
          <div className="system-info-wide"><dt>WSAA</dt><dd>{arca.wsaaUrl}</dd></div>
          <div className="system-info-wide"><dt>WSFE</dt><dd>{arca.wsfeUrl}</dd></div>
          <div><dt>Estado</dt><dd data-ready={arca.listaParaProbar}>{arca.listaParaProbar ? "Listo para probar" : "Configuración incompleta"}</dd></div>
        </dl>
      </section>
      <section>
        <h3>Facturación</h3>
        <div className="system-info-lists">
          <div><h4>Puntos de venta</h4><ul>{info.puntosVenta.map((item) => <li key={item.id}><strong>{item.numero}</strong><span>{item.descripcion}<small>{item.tipoEmision ?? "Sin tipo"} · {item.domicilioComercial ?? "Sin domicilio"}</small></span></li>)}</ul></div>
          <div><h4>Condiciones de IVA</h4><ul>{info.condicionesIva.map((item) => <li key={item.id}><strong>{item.codigoAfip}</strong><span>{item.denominacion}</span></li>)}</ul></div>
          <div><h4>Monedas</h4><ul>{info.monedas.map((item) => <li key={item.id}><strong>{item.codigoAfip}</strong><span>{item.denominacion} · {item.simbolo}</span></li>)}</ul></div>
          <div><h4>Listas de precios</h4><ul>{info.listasPrecio.map((item) => <li key={item.id}><strong>{item.codigo}</strong><span>{item.denominacion}</span></li>)}</ul></div>
        </div>
      </section>
      <section className="system-info-user"><h3>Usuario operativo</h3><p>{info.usuario ? `${info.usuario.nombreCompleto} · ${info.usuario.usuario} · ID ${info.usuario.id}` : "No hay usuario administrativo configurado"}</p></section>
    </div>}
  </Modal>;
}
