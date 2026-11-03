import { useEffect } from "react";
import { createPortal } from "react-dom";
import type { FacturaReporteDTO } from "./models/factura.dto";
import type { InvoiceAction } from "./invoice-actions";

interface InvoiceContextMenuProps {
  invoice: FacturaReporteDTO;
  x: number;
  y: number;
  onAction: (action: InvoiceAction, invoice: FacturaReporteDTO) => void;
  onClose: () => void;
}

export function InvoiceContextMenu({ invoice, x, y, onAction, onClose }: InvoiceContextMenuProps) {
  useEffect(() => {
    const close = () => onClose();
    const closeWithEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("resize", close);
    window.addEventListener("scroll", close, true);
    document.addEventListener("pointerdown", close);
    document.addEventListener("keydown", closeWithEscape);
    return () => {
      window.removeEventListener("resize", close);
      window.removeEventListener("scroll", close, true);
      document.removeEventListener("pointerdown", close);
      document.removeEventListener("keydown", closeWithEscape);
    };
  }, [onClose]);

  const run = (action: InvoiceAction) => {
    onAction(action, invoice);
    onClose();
  };

  const pending = invoice.estado.trim().toUpperCase() === "SIN_EMITIR";
  const issued = invoice.estado.trim().toUpperCase() === "EMITIDA";

  return createPortal(
    <div
      className="context-menu"
      role="menu"
      aria-label={`Acciones de la factura ${invoice.numeroFactura ?? `borrador ${invoice.id}`}`}
      style={{ left: x, top: y }}
      onPointerDown={(event) => event.stopPropagation()}
      onContextMenu={(event) => event.preventDefault()}
    >
      <button type="button" role="menuitem" onClick={() => run("view")}>
        <i className="ph ph-eye" aria-hidden="true" /><span>Ver factura</span>
      </button>
      {pending && <button type="button" role="menuitem" onClick={() => run("emit")}>
        <i className="ph ph-seal-check" aria-hidden="true" /><span>Validar y emitir</span>
      </button>}
      {pending && <button type="button" role="menuitem" onClick={() => run("edit")}>
        <i className="ph ph-pencil-simple" aria-hidden="true" /><span>Editar</span>
      </button>}
      {pending && <button type="button" role="menuitem" data-danger="true" onClick={() => run("delete")}>
        <i className="ph ph-trash" aria-hidden="true" /><span>Eliminar</span>
      </button>}
      {issued && <button type="button" role="menuitem" data-danger="true" onClick={() => run("annul")}>
        <i className="ph ph-prohibit" aria-hidden="true" /><span>Anular</span>
      </button>}
    </div>,
    document.body,
  );
}
