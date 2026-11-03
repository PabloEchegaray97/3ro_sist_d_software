import { useEffect } from "react";
import { createPortal } from "react-dom";

interface EntityContextMenuProps {
  label: string;
  x: number;
  y: number;
  onEdit: () => void;
  onDelete: () => void;
  onClose: () => void;
}

export function EntityContextMenu({
  label,
  x,
  y,
  onEdit,
  onDelete,
  onClose,
}: EntityContextMenuProps) {
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

  const run = (action: () => void) => {
    action();
    onClose();
  };

  return createPortal(
    <div
      className="context-menu"
      role="menu"
      aria-label={`Acciones de ${label}`}
      style={{ left: x, top: y }}
      onPointerDown={(event) => event.stopPropagation()}
      onContextMenu={(event) => event.preventDefault()}
    >
      <button type="button" role="menuitem" onClick={() => run(onEdit)}>
        <i className="ph ph-pencil-simple" aria-hidden="true" /><span>Editar</span>
      </button>
      <button type="button" role="menuitem" data-danger="true" onClick={() => run(onDelete)}>
        <i className="ph ph-trash" aria-hidden="true" /><span>Eliminar</span>
      </button>
    </div>,
    document.body,
  );
}
