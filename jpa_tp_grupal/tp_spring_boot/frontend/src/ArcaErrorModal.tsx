import type { ArcaEmisionDTO } from "./models/factura.dto";
import { Modal } from "./Modal";

interface ArcaErrorModalProps {
  result: ArcaEmisionDTO | null;
  onClose: () => void;
}

export function ArcaErrorModal({ result, onClose }: ArcaErrorModalProps) {
  return (
    <Modal open={result != null} title="ARCA no pudo emitir la factura" className="arca-error-dialog" onClose={onClose}>
      {result && (
        <div className="arca-error-content">
          <p>{result.mensaje}</p>
          {result.mensajes.length > 0 && (
            <ul className="arca-message-list">
              {result.mensajes.map((message, index) => (
                <li key={`${message.tipo}-${message.codigo}-${index}`}>
                  <span>{message.tipo}{message.codigo ? ` ${message.codigo}` : ""}</span>
                  <p>{message.descripcion}</p>
                </li>
              ))}
            </ul>
          )}
          <footer className="dialog-actions">
            <button type="button" data-variant="primary" onClick={onClose}>Entendido</button>
          </footer>
        </div>
      )}
    </Modal>
  );
}
