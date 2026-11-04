import type { PendingInvoiceAction } from "./invoice-actions";
import { Modal } from "./Modal";

interface ConfirmationModalProps {
  pending: PendingInvoiceAction | null;
  busy: boolean;
  onConfirm: () => void;
  onClose: () => void;
}

export function ConfirmationModal({ pending, busy, onConfirm, onClose }: ConfirmationModalProps) {
  if (!pending) return null;
  const deletingAnnulled = pending.invoice.estado.trim().toUpperCase() === "ANULADA";
  return (
    <Modal
      open
      title="Eliminar factura"
      onClose={onClose}
    >
      <div className="confirmation-dialog-content">
        <p className="confirmation-text">
          {deletingAnnulled
            ? `La factura anulada ${pending.invoice.numeroFactura ?? pending.invoice.id} y sus detalles se eliminarán definitivamente. Esta acción no se puede deshacer.`
            : `El borrador ${pending.invoice.numeroFactura ?? pending.invoice.id} y sus detalles se eliminarán definitivamente.`}
        </p>
        <footer className="dialog-actions">
          <button type="button" disabled={busy} onClick={onClose}>Cancelar</button>
          <button type="button" disabled={busy} data-variant="danger" onClick={onConfirm}>
            <i className="ph ph-trash" aria-hidden="true" />
            <span>{busy ? "Procesando…" : "Confirmar"}</span>
          </button>
        </footer>
      </div>
    </Modal>
  );
}
