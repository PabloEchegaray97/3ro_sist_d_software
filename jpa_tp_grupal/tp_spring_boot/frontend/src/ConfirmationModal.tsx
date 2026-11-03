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
  const deleting = pending.action === "delete";
  return (
    <Modal
      open
      title={deleting ? "Eliminar factura" : "Anular factura"}
      onClose={onClose}
    >
      <p className="confirmation-text">
        {deleting
          ? `El borrador ${pending.invoice.numeroFactura ?? pending.invoice.id} y sus detalles se eliminarán definitivamente.`
          : `La factura ${pending.invoice.numeroFactura ?? pending.invoice.id} quedará anulada en el sistema.`}
      </p>
      <footer className="dialog-actions">
        <button type="button" disabled={busy} onClick={onClose}>Cancelar</button>
        <button type="button" disabled={busy} data-variant={deleting ? "danger" : "primary"} onClick={onConfirm}>
          <i className={`ph ${deleting ? "ph-trash" : "ph-prohibit"}`} aria-hidden="true" />
          <span>{busy ? "Procesando…" : "Confirmar"}</span>
        </button>
      </footer>
    </Modal>
  );
}
