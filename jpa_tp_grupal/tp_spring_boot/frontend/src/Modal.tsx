import { useEffect, useRef, type ReactNode } from "react";
import { IconButton } from "./IconButton";

interface ModalProps {
  open: boolean;
  title: string;
  description?: string;
  className?: string;
  children: ReactNode;
  onClose: () => void;
  closeDisabled?: boolean;
}

export function Modal({ open, title, description, className = "", children, onClose, closeDisabled = false }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
      window.dispatchEvent(new Event("app-dialog-stack-change"));
    }
    if (!open && dialog.open) {
      dialog.close();
      window.dispatchEvent(new Event("app-dialog-stack-change"));
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      className={className}
      onCancel={(event) => {
        event.preventDefault();
        if (!closeDisabled) onClose();
      }}
    >
      <header className={`dialog-heading${description ? " has-description" : ""}`}>
        <div>
          <h2>{title}</h2>
          {description && <p>{description}</p>}
        </div>
        <IconButton type="button" icon="ph-x" label="Cerrar" disabled={closeDisabled} onClick={onClose} />
      </header>
      {children}
    </dialog>
  );
}
