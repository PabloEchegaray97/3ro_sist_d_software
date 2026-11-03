import type { FacturaReporteDTO } from "./models/factura.dto";
import { money } from "./formatters";
import { Modal } from "./Modal";

interface EmitInvoiceModalProps {
  invoice: FacturaReporteDTO | null;
  busy: boolean;
  onConfirm: () => void;
  onClose: () => void;
}

export function EmitInvoiceModal({ invoice, busy, onConfirm, onClose }: EmitInvoiceModalProps) {
  return (
    <Modal open={invoice != null} title="Emitir factura" onClose={onClose}>
      {invoice && (
        <div className="emit-confirmation">
          <p>Se validará el comprobante con ARCA y, si es aprobado, se le asignará el CAE.</p>
          <footer className="dialog-actions">
            <button type="button" disabled={busy} onClick={onClose}>Cancelar</button>
            <button type="button" className="emit-confirm-button" data-variant="primary" disabled={busy} onClick={onConfirm}>
              <span className="emit-confirm-label"><i className="ph ph-seal-check" aria-hidden="true" />{busy ? "Emitiendo…" : "Emitir"}</span>
              <strong className="emit-confirm-total">{money(invoice.importeTotal)}</strong>
            </button>
          </footer>
        </div>
      )}
    </Modal>
  );
}
