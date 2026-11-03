import { useCallback, useEffect, useRef, useState } from "react";
import { sileo } from "sileo";
import type { ArcaConfiguracionDTO, ArcaEmisionDTO, FacturaFiltrosDTO, FacturaReporteDTO } from "./models/factura.dto";
import { FacturaService } from "./services/FacturaService";
import type { InvoiceAction, PendingInvoiceAction } from "./invoice-actions";
import { errorMessage } from "./formatters";
import { ConfirmationModal } from "./ConfirmationModal";
import { InvoiceFiltersModal } from "./InvoiceFiltersModal";
import { InvoiceFormModal } from "./InvoiceFormModal";
import { InvoiceTable } from "./InvoiceTable";
import { InvoiceViewModal } from "./InvoiceViewModal";
import { AppSidebar, type InvoiceSection } from "./AppSidebar";
import { ArcaErrorModal } from "./ArcaErrorModal";
import { EmitInvoiceModal } from "./EmitInvoiceModal";
import { ClientsView } from "./ClientsView";
import { ArticlesView } from "./ArticlesView";
import { CatalogView } from "./CatalogView";
import { SystemInfoModal } from "./SystemInfoModal";
import { ToastLayer } from "./ToastLayer";

const service = new FacturaService();

const sectionFromPath = (): InvoiceSection => {
  if (window.location.pathname === "/emitidas") return "issued";
  if (window.location.pathname === "/clientes") return "clients";
  if (window.location.pathname === "/articulos") return "articles";
  if (window.location.pathname === "/articulos/marcas") return "brands";
  if (window.location.pathname === "/articulos/rubros") return "categories";
  return "pending";
};

export function App() {
  const [invoices, setInvoices] = useState<FacturaReporteDTO[]>([]);
  const [section, setSection] = useState<InvoiceSection>(sectionFromPath);
  const [filters, setFilters] = useState<FacturaFiltrosDTO>({});
  const [loading, setLoading] = useState(true);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [formMode, setFormMode] = useState<"new" | number | null>(null);
  const [viewId, setViewId] = useState<number | null>(null);
  const [pending, setPending] = useState<PendingInvoiceAction | null>(null);
  const [actionBusy, setActionBusy] = useState(false);
  const [emittingId, setEmittingId] = useState<number | null>(null);
  const [arcaError, setArcaError] = useState<ArcaEmisionDTO | null>(null);
  const [arcaConfiguration, setArcaConfiguration] = useState<ArcaConfiguracionDTO | null>(null);
  const [arcaConfigurationError, setArcaConfigurationError] = useState(false);
  const [emitCandidate, setEmitCandidate] = useState<FacturaReporteDTO | null>(null);
  const [infoOpen, setInfoOpen] = useState(false);
  const invoiceRequestVersion = useRef(0);

  const loadInvoices = useCallback(async () => {
    const version = ++invoiceRequestVersion.current;
    if (section !== "pending" && section !== "issued") {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const loaded = await service.obtenerFacturas({
        ...filters,
        estado: section === "pending" ? "SIN_EMITIR" : "EMITIDA",
      });
      if (version !== invoiceRequestVersion.current) return;
      setInvoices(loaded);
    } catch (error) {
      if (version !== invoiceRequestVersion.current) return;
      sileo.error({ title: "No se pudieron cargar las facturas", description: errorMessage(error) });
    } finally {
      if (version === invoiceRequestVersion.current) setLoading(false);
    }
  }, [filters, section]);

  useEffect(() => {
    void loadInvoices();
  }, [loadInvoices]);

  useEffect(() => {
    void service.obtenerConfiguracionArca()
      .then((configuracion) => {
        setArcaConfiguration(configuracion);
        setArcaConfigurationError(false);
        console.group("ARCA · configuración del backend");
        console.table({
          Ambiente: configuracion.ambiente,
          CUIT: configuracion.cuit ?? "No configurado",
          "Condición del emisor": configuracion.condicionIvaEmisor,
          "Razón social": configuracion.razonSocialEmisor,
          "Ingresos Brutos": configuracion.ingresosBrutosEmisor,
          "Inicio de actividades": configuracion.inicioActividadesEmisor,
          "Teléfono": configuracion.telefonoEmisor,
          "Logo": configuracion.logoPath || "Pendiente",
          "Tipo de comprobante": `${configuracion.tipoComprobanteDescripcion} (${configuracion.tipoComprobanteCodigo})`,
          "CUIT válido": configuracion.cuitConfigurado,
          "Ruta PKCS#12": configuracion.keystorePath ?? "No configurada",
          "PKCS#12 configurado": configuracion.keystoreConfigurado,
          "PKCS#12 encontrado": configuracion.keystoreExiste,
          "Contraseña configurada": configuracion.passwordConfigurada,
          "WSAA homologación": configuracion.wsaaUrl,
          "WSFE homologación": configuracion.wsfeUrl,
          "Endpoints de homologación": configuracion.endpointsHomologacion,
          "Lista para probar": configuracion.listaParaProbar,
        });
        if (configuracion.listaParaProbar) {
          console.info("ARCA: configuración de homologación cargada correctamente.");
        } else {
          console.error("ARCA: la configuración de homologación está incompleta. Revisá los valores marcados como false.");
        }
        console.groupEnd();
      })
      .catch((error: unknown) => {
        setArcaConfiguration(null);
        setArcaConfigurationError(true);
        console.error("ARCA: no se pudo consultar la configuración del backend.", error);
      });
  }, []);

  useEffect(() => {
    if (!["/sin-emitir", "/emitidas", "/clientes", "/articulos", "/articulos/marcas", "/articulos/rubros"].includes(window.location.pathname)) {
      window.history.replaceState({}, "", "/sin-emitir");
    }
    const syncSection = () => setSection(sectionFromPath());
    window.addEventListener("popstate", syncSection);
    return () => window.removeEventListener("popstate", syncSection);
  }, []);

  const changeSection = useCallback((next: InvoiceSection) => {
    const paths: Record<InvoiceSection, string> = {
      pending: "/sin-emitir",
      issued: "/emitidas",
      clients: "/clientes",
      articles: "/articulos",
      brands: "/articulos/marcas",
      categories: "/articulos/rubros",
    };
    const path = paths[next];
    if (window.location.pathname !== path) window.history.pushState({}, "", path);
    setSection(next);
  }, []);

  const closeForm = useCallback(() => setFormMode(null), []);
  const closeView = useCallback(() => setViewId(null), []);

  const emitInvoice = useCallback(async (invoice: FacturaReporteDTO) => {
    if (emittingId != null) return;
    setEmittingId(invoice.id);
    try {
      const result = await service.emitirFactura(invoice.id);
      if (result.emitida) {
        sileo.success({
          title: "Factura emitida por ARCA",
          description: `Comprobante ${result.numero} · CAE ${result.cae}`,
        });
        await loadInvoices();
      } else {
        setArcaError(result);
        sileo.error({ title: "ARCA rechazó la emisión", description: result.mensaje });
      }
    } catch (error) {
      const message = errorMessage(error);
      setArcaError({
        emitida: false, facturaId: invoice.id, numero: invoice.numeroFactura,
        estado: invoice.estado, cae: null, caeFechaVencimiento: null,
        mensaje: message, mensajes: [{ tipo: "ERROR", codigo: "", descripcion: message }],
      });
      sileo.error({ title: "No se pudo validar con ARCA", description: message });
    } finally {
      setEmittingId(null);
      setEmitCandidate(null);
    }
  }, [emittingId, loadInvoices]);

  const handleAction = useCallback((action: InvoiceAction, invoice: FacturaReporteDTO) => {
    if (action === "view") setViewId(invoice.id);
    if (action === "edit") setFormMode(invoice.id);
    if (action === "emit") setEmitCandidate(invoice);
    if (action === "annul") setPending({ action: "annul", invoice });
    if (action === "delete") setPending({ action: "delete", invoice });
  }, [emitInvoice]);

  const confirmAction = async () => {
    if (!pending) return;
    setActionBusy(true);
    try {
      if (pending.action === "annul") {
        await service.anularFactura(pending.invoice.id);
        sileo.success({ title: "Factura anulada", description: `Comprobante ${pending.invoice.numeroFactura ?? pending.invoice.id}` });
      } else {
        await service.eliminarFactura(pending.invoice.id);
        sileo.success({ title: "Factura eliminada", description: `Borrador ${pending.invoice.numeroFactura ?? pending.invoice.id}` });
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
      <ToastLayer />
      <div className="app-shell">
      <div className="app-layout">
      <AppSidebar
        active={section}
        arcaConfiguration={arcaConfiguration}
        arcaConfigurationError={arcaConfigurationError}
        onChange={changeSection}
        onInfo={() => setInfoOpen(true)}
      />
      <main>
        {section === "clients" ? <ClientsView service={service} />
          : section === "articles" ? <ArticlesView service={service} />
          : section === "brands" ? <CatalogView kind="marcas" service={service} />
          : section === "categories" ? <CatalogView kind="rubros" service={service} /> : (
        <section className="invoice-list">
          <header className="page-heading">
            <div className="section-heading-copy">
              <h1>{section === "pending" ? "Sin emitir" : "Emitidas"}</h1>
              <p>{section === "pending"
                ? "Revisá tus facturas y validalas con ARCA cuando estén listas."
                : "Comprobantes autorizados por ARCA con CAE asignado."}</p>
            </div>
            <div><h1>Comprobantes registrados</h1><p>Consultá, editá, anulá o eliminá las facturas existentes.</p></div>
            <div className="heading-actions">
              <button type="button" onClick={() => setFiltersOpen(true)}><i className="ph ph-funnel" aria-hidden="true" /><span>Filtros</span></button>
              <button type="button" disabled={loading} onClick={() => void loadInvoices()}><i className="ph ph-arrow-clockwise" aria-hidden="true" /><span>Recargar</span></button>
              {section === "pending" && <button type="button" data-variant="primary" onClick={() => setFormMode("new")}><i className="ph ph-plus" aria-hidden="true" /><span>Nueva factura</span></button>}
            </div>
          </header>
          <InvoiceTable invoices={invoices} loading={loading} onAction={handleAction} />
        </section>
        )}
      </main>
      </div>

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
      <ArcaErrorModal result={arcaError} onClose={() => setArcaError(null)} />
      <EmitInvoiceModal
        invoice={emitCandidate}
        busy={emittingId != null}
        onClose={() => { if (emittingId == null) setEmitCandidate(null); }}
        onConfirm={() => { if (emitCandidate) void emitInvoice(emitCandidate); }}
      />
      <SystemInfoModal open={infoOpen} service={service} onClose={() => setInfoOpen(false)} />
      </div>
    </>
  );
}
