import { Modal } from "./Modal";

interface DeactivateEntityModalProps {
  open: boolean;
  entityName: string;
  entityType: "cliente" | "artículo" | "rubro" | "marca";
  busy: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function DeactivateEntityModal({
  open,
  entityName,
  entityType,
  busy,
  onClose,
  onConfirm,
}: DeactivateEntityModalProps) {
  return (
    <Modal open={open} title={`Eliminar ${entityType}`} onClose={onClose}>
      <div className="emit-confirmation">
        <p className="confirmation-text">
          <strong>{entityName}</strong> se marcará como inactivo. El registro y su historial
          permanecerán guardados en la base de datos.
        </p>
        <footer className="dialog-actions">
          <button type="button" disabled={busy} onClick={onClose}>Cancelar</button>
          <button type="button" data-variant="danger" disabled={busy} onClick={onConfirm}>
            <i className="ph ph-trash" aria-hidden="true" />
            <span>{busy ? "Eliminando…" : "Eliminar"}</span>
          </button>
        </footer>
      </div>
    </Modal>
  );
}
