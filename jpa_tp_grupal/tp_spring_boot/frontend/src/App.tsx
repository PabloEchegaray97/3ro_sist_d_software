import { useCallback, useEffect, useState } from "react";
import { sileo, Toaster } from "sileo";
import type { FacturaFiltrosDTO, FacturaReporteDTO } from "./models/factura.dto";
import { FacturaService } from "./services/FacturaService";
import type { InvoiceAction, PendingInvoiceAction } from "./invoice-actions";
import { errorMessage } from "./formatters";
import { ConfirmationModal } from "./ConfirmationModal";
import { AppNavbar } from "./AppNavbar";
import { InvoiceFiltersModal } from "./InvoiceFiltersModal";
import { InvoiceFormModal } from "./InvoiceFormModal";
import { InvoiceTable } from "./InvoiceTable";
import { InvoiceViewModal } from "./InvoiceViewModal";

const service = new FacturaService();

export function App() {
  const [invoices, setInvoices] = useState<FacturaReporteDTO[]>([]);
  const [filters, setFilters] = useState<FacturaFiltrosDTO>({});
  const [loading, setLoading] = useState(true);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [formMode, setFormMode] = useState<"new" | number | null>(null);
  const [viewId, setViewId] = useState<number | null>(null);
  const [pending, setPending] = useState<PendingInvoiceAction | null>(null);
  const [actionBusy, setActionBusy] = useState(false);

  const loadInvoices = useCallback(async () => {
    setLoading(true);
    try {
      setInvoices(await service.obtenerFacturas(filters));
    } catch (error) {
      sileo.error({ title: "No se pudieron cargar las facturas", description: errorMessage(error) });
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    void loadInvoices();
  }, [loadInvoices]);

  const closeForm = useCallback(() => setFormMode(null), []);
  const closeView = useCallback(() => setViewId(null), []);

  const handleAction = useCallback((action: InvoiceAction, invoice: FacturaReporteDTO) => {
    if (action === "view") setViewId(invoice.id);
    if (action === "edit") setFormMode(invoice.id);
    if (action === "annul") setPending({ action: "annul", invoice });
    if (action === "delete") setPending({ action: "delete", invoice });
  }, []);

  const confirmAction = async () => {
    if (!pending) return;
    setActionBusy(true);
    try {
      if (pending.action === "annul") {
        await service.actualizarEstado(pending.invoice.id, "ANULADA", "Anulada desde el frontend");
        sileo.success({ title: "Factura anulada", description: `Comprobante ${pending.invoice.numeroFactura}` });
      } else {
        await service.eliminarFactura(pending.invoice.id);
        sileo.success({ title: "Factura eliminada", description: `Comprobante ${pending.invoice.numeroFactura}` });
      }
      setPending(null);
      await loadInvoices();
    } catch (error) {
      sileo.error({ title: "No se pudo completar la acción", description: errorMessage(error) });
    } finally {
      setActionBusy(false);
    }
  };

  const invoiceSaved = () => {
    setFormMode(null);
    void loadInvoices();
  };

  return (
    <>
      <Toaster position="top-center" />
      <div className="app-shell">
      <AppNavbar onNewInvoice={() => setFormMode("new")} />
      <main>
        <section className="invoice-list">
          <header className="page-heading">
            <div><h1>Comprobantes registrados</h1><p>Consultá, editá, anulá o eliminá las facturas existentes.</p></div>
            <div className="heading-actions">
              <button type="button" onClick={() => setFiltersOpen(true)}><i className="ph ph-funnel" aria-hidden="true" /><span>Filtros</span></button>
              <button type="button" disabled={loading} onClick={() => void loadInvoices()}><i className="ph ph-arrow-clockwise" aria-hidden="true" /><span>Recargar</span></button>
            </div>
          </header>
          <InvoiceTable invoices={invoices} loading={loading} onAction={handleAction} />
        </section>
      </main>

      <InvoiceFiltersModal
        open={filtersOpen}
        filters={filters}
        onClose={() => setFiltersOpen(false)}
        onApply={(nextFilters) => {
          setFilters(nextFilters);
          setFiltersOpen(false);
        }}
      />
      <InvoiceFormModal mode={formMode} service={service} onClose={closeForm} onSaved={invoiceSaved} />
      <InvoiceViewModal invoiceId={viewId} service={service} onClose={closeView} />
      <ConfirmationModal
        pending={pending}
        busy={actionBusy}
        onClose={() => { if (!actionBusy) setPending(null); }}
        onConfirm={() => void confirmAction()}
      />
      </div>
    </>
  );
}
