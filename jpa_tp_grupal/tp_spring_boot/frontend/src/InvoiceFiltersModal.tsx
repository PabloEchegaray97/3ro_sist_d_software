import { useEffect, useState, type FormEvent } from "react";
import type { FacturaFiltrosDTO } from "./models/factura.dto";
import { Modal } from "./Modal";

interface InvoiceFiltersModalProps {
  open: boolean;
  filters: FacturaFiltrosDTO;
  onApply: (filters: FacturaFiltrosDTO) => void;
  onClose: () => void;
}

interface FilterDraft {
  fechaDesde: string;
  fechaHasta: string;
  montoMinimo: string;
}

export function InvoiceFiltersModal({ open, filters, onApply, onClose }: InvoiceFiltersModalProps) {
  const [draft, setDraft] = useState<FilterDraft>({
    fechaDesde: "",
    fechaHasta: "",
    montoMinimo: "",
  });

  useEffect(() => {
    if (!open) return;
    setDraft({
      fechaDesde: filters.fechaDesde ?? "",
      fechaHasta: filters.fechaHasta ?? "",
      montoMinimo: filters.montoMinimo == null ? "" : String(filters.montoMinimo),
    });
  }, [filters, open]);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    onApply({
      fechaDesde: draft.fechaDesde || undefined,
      fechaHasta: draft.fechaHasta || undefined,
      montoMinimo: draft.montoMinimo === "" ? undefined : Number(draft.montoMinimo),
    });
  };

  const clear = () => {
    setDraft({ fechaDesde: "", fechaHasta: "", montoMinimo: "" });
    onApply({});
  };

  return (
    <Modal open={open} title="Filtrar comprobantes" onClose={onClose}>
      <form onSubmit={submit}>
        <fieldset className="filter-grid">
          <label><span>Desde</span><input type="date" value={draft.fechaDesde} onChange={(event) => setDraft({ ...draft, fechaDesde: event.target.value })} /></label>
          <label><span>Hasta</span><input type="date" value={draft.fechaHasta} onChange={(event) => setDraft({ ...draft, fechaHasta: event.target.value })} /></label>
          <label><span>Monto mínimo</span><input type="number" min="0" step="0.01" value={draft.montoMinimo} onChange={(event) => setDraft({ ...draft, montoMinimo: event.target.value })} /></label>
        </fieldset>
        <footer>
          <button type="button" onClick={clear}><i className="ph ph-eraser" aria-hidden="true" /><span>Limpiar</span></button>
          <button type="submit" data-variant="primary"><i className="ph ph-check" aria-hidden="true" /><span>Aplicar</span></button>
        </footer>
      </form>
    </Modal>
  );
}
