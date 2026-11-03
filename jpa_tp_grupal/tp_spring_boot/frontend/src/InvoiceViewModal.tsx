import { useEffect, useState } from "react";
import { sileo } from "sileo";
import type { FacturaVistaDTO } from "./models/factura.dto";
import { FacturaService } from "./services/FacturaService";
import { dateTimeParts, decimal, errorMessage, formatArcaResult, formatStatus, money } from "./formatters";
import { Modal } from "./Modal";

interface InvoiceViewModalProps {
  invoiceId: number | null;
  service: FacturaService;
  onClose: () => void;
}

export function InvoiceViewModal({ invoiceId, service, onClose }: InvoiceViewModalProps) {
  const [invoice, setInvoice] = useState<FacturaVistaDTO | null>(null);

  useEffect(() => {
    if (invoiceId == null) return;
    let active = true;
    setInvoice(null);
    service.obtenerFactura(invoiceId)
      .then((result) => { if (active) setInvoice(result); })
      .catch((error) => {
        if (!active) return;
        sileo.error({ title: "No se pudo cargar la factura", description: errorMessage(error) });
        onClose();
      });
    return () => { active = false; };
  }, [invoiceId, onClose, service]);

  const date = invoice ? dateTimeParts(invoice.fechaEmision) : null;

  return (
    <Modal
      open={invoiceId != null}
      title={invoice ? (invoice.numero == null ? `Borrador ${invoice.id}` : `Factura ${invoice.numero}`) : "Detalle de factura"}
      description="Información registrada del comprobante."
      className="wide-dialog"
      onClose={onClose}
    >
      {!invoice && <p data-empty>Cargando factura…</p>}
      {invoice && (
        <div className="invoice-view">
          <dl className="invoice-summary">
            <div><dt>Número</dt><dd>{invoice.numero ?? "Se asigna al emitir"}</dd></div>
            <div><dt>Fecha</dt><dd>{date?.date} {date?.time}</dd></div>
            <div><dt>Estado</dt><dd data-status={invoice.estado.toUpperCase() === "ANULADA" ? "anulada" : "emitida"}>{formatStatus(invoice.estado)}</dd></div>
            <div><dt>Punto de venta</dt><dd>{invoice.puntoVentaDescripcion} · ID {invoice.puntoVentaId}</dd></div>
            <div><dt>Cliente</dt><dd>{invoice.clienteDenominacion}{invoice.clienteId == null ? "" : ` · ID ${invoice.clienteId}`}</dd></div>
            <div><dt>Lista de precios</dt><dd>{invoice.listaPrecio ?? "Sin lista"}{invoice.listaPrecioCodigo ? ` · ${invoice.listaPrecioCodigo}` : ""}</dd></div>
            <div><dt>Condición de IVA</dt><dd>{invoice.condicionIva} · ID {invoice.condicionIvaId}</dd></div>
            <div><dt>Moneda</dt><dd>{invoice.tipoMoneda} · ID {invoice.tipoMonedaId}</dd></div>
            <div><dt>Usuario</dt><dd>{invoice.usuario} · ID {invoice.usuarioId}</dd></div>
            {invoice.fechaAnulacion && <div><dt>Fecha de anulación</dt><dd>{invoice.fechaAnulacion}</dd></div>}
          </dl>
          <div className="invoice-notes"><span>Observaciones</span><p>{invoice.observaciones || "Sin observaciones"}</p></div>
          {invoice.cae && (
            <dl className="invoice-arca-summary">
              <div><dt>CAE</dt><dd>{invoice.cae}</dd></div>
              <div><dt>Vencimiento CAE</dt><dd>{invoice.caeFechaVencimiento ?? "—"}</dd></div>
              <div><dt>Resultado ARCA</dt><dd>{formatArcaResult(invoice.resultadoAfip)}</dd></div>
            </dl>
          )}
          {!invoice.cae && invoice.motivoRechazo && (
            <div className="invoice-arca-error"><span>Último resultado de ARCA</span><p>{invoice.motivoRechazo}</p></div>
          )}
          <div className="table-shell">
            <table className="invoice-details">
              <thead><tr><th>Artículo</th><th>Lista</th><th>Descripción</th><th>Cantidad</th><th>Precio</th><th>Bonif.</th><th>IVA</th><th>Neto</th><th>IVA importe</th><th>Subtotal</th></tr></thead>
              <tbody>{invoice.detalles.map((detail) => (
                <tr key={detail.id}>
                  <td>{detail.articuloCodigo}<small>{detail.articuloDenominacion} · ID {detail.articuloId}</small></td>
                  <td>{detail.listaPrecio}<small>ID {detail.listaPrecioArticuloId}</small></td>
                  <td>{detail.descripcion ?? ""}</td><td>{decimal(detail.cantidad)}</td>
                  <td data-money>{money(detail.precioUnitario)}</td><td>{decimal(detail.porcentajeBonificacion)} %</td>
                  <td>{decimal(detail.porcentajeIva)} %</td><td data-money>{money(detail.importeNeto)}</td>
                  <td data-money>{money(detail.importeIva)}</td><td data-money>{money(detail.importeSubtotal)}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
          <dl className="invoice-totals">
            <div><dt>Total</dt><dd>{money(invoice.importeTotal)}</dd></div>
            <div><dt>Cobrado</dt><dd>{money(invoice.importeCobrado)}</dd></div>
            <div><dt>Saldo</dt><dd>{money(invoice.importeSaldo)}</dd></div>
          </dl>
          {invoice.cae && <footer className="invoice-print-actions">
            <button
              type="button"
              className="outlined-button"
              onClick={() => {
                if (!service.abrirFacturaPdf(invoice.id)) {
                  sileo.warning({ title: "El navegador bloqueó la ventana del PDF" });
                }
              }}
            >
              <i className="ph ph-printer" aria-hidden="true" /><span>Imprimir</span>
            </button>
          </footer>}
        </div>
      )}
    </Modal>
  );
}
