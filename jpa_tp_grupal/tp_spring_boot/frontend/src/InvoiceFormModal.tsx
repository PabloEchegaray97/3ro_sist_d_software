import { useEffect, useMemo, useState, type FormEvent } from "react";
import { sileo } from "sileo";
import type {
  ArticuloDisponibleDTO,
  ClienteDisponibleDTO,
  FacturaDetalleRequestDTO,
  FacturaOpcionesDTO,
  FacturaRequestDTO,
  FacturaVistaDTO,
} from "./models/factura.dto";
import { FacturaService } from "./services/FacturaService";
import { dateTimeParts, errorMessage, localDateTime, money } from "./formatters";
import { calculateTotal, detailFromArticle } from "./invoice-utils";
import { IconButton } from "./IconButton";
import { Modal } from "./Modal";
import { ClientExplorerModal } from "./ClientExplorerModal";
import { PriceArticleExplorerModal } from "./PriceArticleExplorerModal";

interface InvoiceFormModalProps {
  mode: "new" | number | null;
  service: FacturaService;
  onSaved: () => void;
  onClose: () => void;
}

interface HeaderDraft {
  numero: string;
  fechaEmision: string;
  puntoVentaId: string;
  clienteId: string;
  listaPrecioId: string;
  condicionIvaId: string;
  tipoMonedaId: string;
  observaciones: string;
}

const emptyHeader: HeaderDraft = {
  numero: "",
  fechaEmision: localDateTime(),
  puntoVentaId: "",
  clienteId: "",
  listaPrecioId: "",
  condicionIvaId: "",
  tipoMonedaId: "",
  observaciones: "",
};

export function InvoiceFormModal({ mode, service, onSaved, onClose }: InvoiceFormModalProps) {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [options, setOptions] = useState<FacturaOpcionesDTO | null>(null);
  const [articles, setArticles] = useState<ArticuloDisponibleDTO[]>([]);
  const [selectedClient, setSelectedClient] = useState<ClienteDisponibleDTO | null>(null);
  const [editing, setEditing] = useState<FacturaVistaDTO | null>(null);
  const [header, setHeader] = useState<HeaderDraft>(emptyHeader);
  const [details, setDetails] = useState<FacturaDetalleRequestDTO[]>([]);
  const [headerCollapsed, setHeaderCollapsed] = useState(false);
  const [clientExplorerOpen, setClientExplorerOpen] = useState(false);
  const [articleExplorerOpen, setArticleExplorerOpen] = useState(false);

  useEffect(() => {
    if (mode == null) return;
    let active = true;
    setLoading(true);
    setOptions(null);
    setArticles([]);
    setSelectedClient(null);
    setEditing(null);
    setDetails([]);
    setHeaderCollapsed(false);
    setHeader({ ...emptyHeader, fechaEmision: localDateTime() });

    Promise.all([
      service.obtenerOpcionesFactura(),
      mode === "new" ? Promise.resolve(null) : service.obtenerFactura(mode),
    ]).then(([loadedOptions, invoice]) => {
      if (!active) return;
      setOptions(loadedOptions);
      setEditing(invoice);
      if (invoice) {
        setSelectedClient(invoice.clienteId == null ? null : {
          id: invoice.clienteId,
          cuitCuil: "",
          denominacion: invoice.clienteDenominacion,
          condicionIvaId: invoice.condicionIvaId,
          condicionIva: invoice.condicionIva,
          email: null,
          telefono: null,
          celular: null,
          nombreCalle: null,
          numeroCalle: null,
          listaPrecioId: invoice.listaPrecioId,
          listaPrecioCodigo: invoice.listaPrecioCodigo,
          listaPrecio: invoice.listaPrecio,
          fechaAlta: "",
          ultimaCompra: null,
        });
        if (invoice.clienteId != null) {
          void service.buscarClientesDisponibles(
            String(invoice.clienteId),
            0,
            8,
            "ULTIMOS_AGREGADOS",
          ).then((page) => {
            if (!active) return;
            const client = page.contenido.find((item) => item.id === invoice.clienteId);
            if (client) setSelectedClient(client);
          }).catch(() => undefined);
        }
        setHeader({
          numero: String(invoice.numero),
          fechaEmision: invoice.fechaEmision.slice(0, 16),
          puntoVentaId: String(invoice.puntoVentaId),
          clienteId: invoice.clienteId == null ? "" : String(invoice.clienteId),
          listaPrecioId: String(invoice.listaPrecioId ?? loadedOptions.listaPrecioConsumidorFinal?.id ?? ""),
          condicionIvaId: String(invoice.condicionIvaId),
          tipoMonedaId: String(invoice.tipoMonedaId),
          observaciones: invoice.observaciones ?? "",
        });
        setDetails(invoice.detalles.map((detail) => ({
          articuloId: detail.articuloId,
          listaPrecioArticuloId: detail.listaPrecioArticuloId,
          descripcion: detail.descripcion,
          cantidad: detail.cantidad,
          precioUnitario: detail.precioUnitario,
          porcentajeBonificacion: detail.porcentajeBonificacion,
          porcentajeIva: detail.porcentajeIva,
        })));
        setHeaderCollapsed(invoice.detalles.length > 0);
      } else {
        const consumidorFinal = loadedOptions.condicionesIva.find((condition) =>
          condition.descripcion.toLowerCase().includes("consumidor final"),
        );
        setHeader({
          ...emptyHeader,
          numero: String(loadedOptions.siguienteNumero),
          fechaEmision: localDateTime(),
          puntoVentaId: loadedOptions.puntosVenta[0] ? String(loadedOptions.puntosVenta[0].id) : "",
          listaPrecioId: String(loadedOptions.listaPrecioConsumidorFinal?.id ?? ""),
          condicionIvaId: consumidorFinal ? String(consumidorFinal.id) : String(loadedOptions.condicionesIva[0]?.id ?? ""),
          tipoMonedaId: loadedOptions.tiposMoneda[0] ? String(loadedOptions.tiposMoneda[0].id) : "",
        });
      }
    }).catch((error) => {
      if (!active) return;
      sileo.error({ title: "No se pudo preparar la factura", description: errorMessage(error) });
      onClose();
    }).finally(() => {
      if (active) setLoading(false);
    });

    return () => { active = false; };
  }, [mode, onClose, service]);

  const total = useMemo(() => calculateTotal(details, articles), [articles, details]);
  const summaryDate = dateTimeParts(header.fechaEmision);
  const summaryPoint = options?.puntosVenta.find((option) => String(option.id) === header.puntoVentaId)?.descripcion ?? "Sin punto de venta";
  const summaryTaxCondition = options?.condicionesIva.find((option) => String(option.id) === header.condicionIvaId)?.descripcion ?? "Sin condición de IVA";
  const summaryCurrency = options?.tiposMoneda.find((option) => String(option.id) === header.tipoMonedaId)?.descripcion ?? "Sin moneda";
  const summaryPriceList = editing?.listaPrecioId === Number(header.listaPrecioId)
    ? editing.listaPrecio ?? "Sin lista de precios"
    : selectedClient?.listaPrecioId === Number(header.listaPrecioId)
      ? selectedClient.listaPrecio ?? "Sin lista de precios"
      : options?.listasPrecio.find((list) => list.id === Number(header.listaPrecioId))?.descripcion
        ?? "Sin lista de precios";
  const summaryUser = options?.usuario?.descripcion ?? "Sin usuario";
  const ready = Boolean(
    options?.usuario
    && options.puntosVenta.length
    && options.condicionesIva.length
    && options.tiposMoneda.length
    && options.listasPrecio.length
    && header.listaPrecioId !== "",
  );

  const changeClient = (client: ClienteDisponibleDTO | null) => {
    const consumidorFinal = options?.condicionesIva.find((condition) =>
      condition.descripcion.toLowerCase().includes("consumidor final"),
    );
    const conditionId = client == null ? consumidorFinal?.id : client.condicionIvaId;
    const nextPriceListId = client?.listaPrecioId ?? options?.listaPrecioConsumidorFinal?.id ?? null;
    if (details.length > 0 && String(nextPriceListId ?? "") !== header.listaPrecioId) {
      setDetails([]);
      setArticles([]);
      setHeaderCollapsed(false);
      sileo.warning({
        title: "Se actualizó la lista de precios",
        description: "Se quitaron los artículos para evitar mezclar precios de listas diferentes.",
      });
    }
    setSelectedClient(client);
    setHeader({
      ...header,
      clienteId: client == null ? "" : String(client.id),
      listaPrecioId: String(nextPriceListId ?? ""),
      condicionIvaId: conditionId == null ? header.condicionIvaId : String(conditionId),
    });
  };

  const changePriceList = (listaPrecioId: string) => {
    if (details.length > 0 && listaPrecioId !== header.listaPrecioId) {
      setDetails([]);
      setArticles([]);
      setHeaderCollapsed(false);
      sileo.warning({
        title: "Se actualizó la lista de precios",
        description: "Se quitaron los artículos para evitar mezclar precios de listas diferentes.",
      });
    }
    setHeader({ ...header, listaPrecioId });
  };

  const addArticle = (article: ArticuloDisponibleDTO) => {
    setArticles((current) => current.some(
      (item) => item.listaPrecioArticuloId === article.listaPrecioArticuloId,
    ) ? current : [...current, article]);
    setDetails((current) => {
      const existingIndex = current.findIndex(
        (detail) => detail.listaPrecioArticuloId === article.listaPrecioArticuloId,
      );
      if (existingIndex === -1) return [...current, detailFromArticle(article)];
      return current.map((detail, index) => index === existingIndex
        ? { ...detail, cantidad: detail.cantidad + 1 }
        : detail);
    });
    setHeaderCollapsed(true);
  };

  const removeDetail = (index: number) => {
    if (details.length === 1) setHeaderCollapsed(false);
    setDetails((current) => current.filter((_, currentIndex) => currentIndex !== index));
  };

  const openArticleExplorer = () => {
    if (!header.listaPrecioId) {
      sileo.error({ title: "El cliente no tiene una lista de precios asignada" });
      return;
    }
    setHeaderCollapsed(true);
    setArticleExplorerOpen(true);
  };

  const updateDetail = (
    index: number,
    field: keyof FacturaDetalleRequestDTO,
    value: string,
  ) => {
    setDetails((current) => current.map((detail, currentIndex) => {
      if (currentIndex !== index) return detail;
      if (field === "descripcion") return { ...detail, descripcion: value || null };
      const nullable = field === "precioUnitario" || field === "porcentajeBonificacion" || field === "porcentajeIva";
      return { ...detail, [field]: value === "" && nullable ? null : Number(value) };
    }));
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!ready || !options?.usuario) {
      sileo.error({ title: "Faltan datos maestros obligatorios" });
      return;
    }
    if (details.length === 0) {
      sileo.error({ title: "Agregá al menos un ítem a la factura" });
      return;
    }
    const request: FacturaRequestDTO = {
      numero: Number(header.numero),
      fechaEmision: header.fechaEmision,
      puntoVentaId: Number(header.puntoVentaId),
      clienteId: header.clienteId === "" ? null : Number(header.clienteId),
      listaPrecioId: Number(header.listaPrecioId),
      condicionIvaId: Number(header.condicionIvaId),
      tipoMonedaId: Number(header.tipoMonedaId),
      usuarioId: options.usuario.id,
      estado: editing?.estado ?? "EMITIDA",
      importeCobrado: total,
      observaciones: header.observaciones || null,
      detalles: details,
    };

    setSaving(true);
    try {
      const saved = editing
        ? await service.actualizarFactura(editing.id, request)
        : await service.crearFactura(request);
      sileo.success({
        title: editing ? "Factura actualizada" : "Factura creada",
        description: `Comprobante ${saved.numeroFactura}`,
      });
      onSaved();
    } catch (error) {
      sileo.error({ title: "No se pudo guardar la factura", description: errorMessage(error) });
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
    <Modal
      open={mode != null}
      title={mode === "new" ? "Nueva factura" : "Editar factura"}
      className="invoice-form-dialog"
      onClose={onClose}
    >
      {loading && <p data-empty>Cargando datos de la factura…</p>}
      {!loading && (
        <form onSubmit={submit}>
          {headerCollapsed && details.length > 0 ? (
            <div
              className="invoice-header-readonly"
              role="button"
              tabIndex={0}
              title="Editar datos del comprobante"
              onClick={() => setHeaderCollapsed(false)}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  setHeaderCollapsed(false);
                }
              }}
            >
              <div className="invoice-section-heading">
                <strong>Datos del comprobante</strong>
                <span className="invoice-edit-hint"><i className="ph ph-pencil-simple" aria-hidden="true" /> Editar</span>
              </div>
              <div className="invoice-form-grid invoice-readonly-grid">
                <div className="field"><span>Número</span><span className="invoice-text-value">{header.numero}</span></div>
                <div className="field"><span>Fecha y hora</span><span className="invoice-text-value">{summaryDate.date} {summaryDate.time}</span></div>
                <div className="field"><span>Punto de venta</span><span className="invoice-text-value">{summaryPoint}</span></div>
                <div className="field"><span>Cliente</span><span className="invoice-text-value">{selectedClient?.denominacion ?? "Consumidor final"}{selectedClient?.cuitCuil && <small>{selectedClient.cuitCuil}</small>}</span></div>
                <div className="field"><span>Lista de precios</span><span className="invoice-text-value">{summaryPriceList}</span></div>
                <div className="field"><span>Condición de IVA</span><span className="invoice-text-value">{summaryTaxCondition}</span></div>
                <div className="field"><span>Tipo de moneda</span><span className="invoice-text-value">{summaryCurrency}</span></div>
                <div className="field"><span>Usuario</span><span className="invoice-text-value">{summaryUser}</span></div>
              </div>
              <div className="field invoice-readonly-notes"><span>Observaciones</span><span className="invoice-text-value">{header.observaciones || "Sin observaciones"}</span></div>
            </div>
          ) : (
            <div className="invoice-header-editor">
              <div className="invoice-section-heading">
                <strong>Datos del comprobante</strong>
              </div>
              <fieldset className="invoice-form-grid">
                <label><span>Número</span><input type="number" min="1" readOnly required value={header.numero} /></label>
                <label><span>Fecha y hora</span><input type="datetime-local" required value={header.fechaEmision} onChange={(event) => setHeader({ ...header, fechaEmision: event.target.value })} /></label>
                <label><span>Punto de venta</span><select required value={header.puntoVentaId} onChange={(event) => setHeader({ ...header, puntoVentaId: event.target.value })}>{options?.puntosVenta.map((option) => <option key={option.id} value={option.id}>{option.descripcion}</option>)}</select></label>
                <div className="field">
                  <span>Cliente</span>
                  <div className="explorer-field client-explorer-field">
                    <div className="client-selected-value">
                      <span className="client-selected-copy">
                        <strong>{selectedClient?.denominacion ?? "Consumidor final"}</strong>
                        {selectedClient?.cuitCuil && <small>{selectedClient.cuitCuil}</small>}
                      </span>
                      {selectedClient && <IconButton type="button" className="client-clear" icon="ph-x" label="Quitar cliente" onClick={() => changeClient(null)} />}
                    </div>
                    <button type="button" onClick={() => setClientExplorerOpen(true)}><i className="ph ph-magnifying-glass" aria-hidden="true" /><span>Buscar</span></button>
                  </div>
                </div>
                <label><span>Lista de precios</span><select required value={header.listaPrecioId} onChange={(event) => changePriceList(event.target.value)}>{options?.listasPrecio.map((option) => <option key={option.id} value={option.id}>{option.descripcion}</option>)}</select></label>
                <label><span>Condición de IVA</span><select disabled required value={header.condicionIvaId}>{options?.condicionesIva.map((option) => <option key={option.id} value={option.id}>{option.descripcion}</option>)}</select></label>
                <label><span>Tipo de moneda</span><select required value={header.tipoMonedaId} onChange={(event) => setHeader({ ...header, tipoMonedaId: event.target.value })}>{options?.tiposMoneda.map((option) => <option key={option.id} value={option.id}>{option.descripcion}</option>)}</select></label>
                <label><span>Usuario</span><input readOnly value={options?.usuario?.descripcion ?? "No existe el usuario admin"} /></label>
              </fieldset>
              <label><span>Observaciones</span><textarea rows={3} value={header.observaciones} onChange={(event) => setHeader({ ...header, observaciones: event.target.value })} /></label>
            </div>
          )}
          <div className={`details-list${details.length > 0 ? " has-details" : " empty"}`}>
            <div className="table-shell">
            <table className={`details-editor${details.length === 0 ? " empty" : ""}`}>
              <thead><tr><th>Artículo ID</th><th>Precio lista ID</th><th>Descripción</th><th>Cantidad</th><th>Precio</th><th>Bonif. %</th><th>IVA %</th><th></th></tr></thead>
              <tbody>
                {details.length === 0 && <tr><td colSpan={8} data-empty><div className="details-empty-content"><span>Aún no hay artículos</span><button type="button" className="outlined-button" onClick={openArticleExplorer}><i className="ph ph-plus" aria-hidden="true" /><span>Agregar artículo</span></button></div></td></tr>}
                {details.map((detail, index) => (
                  <tr key={`${detail.listaPrecioArticuloId}-${index}`}>
                    <td><input aria-label="Artículo ID" type="number" min="1" required value={detail.articuloId || ""} onChange={(event) => updateDetail(index, "articuloId", event.target.value)} /></td>
                    <td><input aria-label="Precio lista ID" type="number" min="1" required value={detail.listaPrecioArticuloId || ""} onChange={(event) => updateDetail(index, "listaPrecioArticuloId", event.target.value)} /></td>
                    <td><input aria-label="Descripción" value={detail.descripcion ?? ""} onChange={(event) => updateDetail(index, "descripcion", event.target.value)} /></td>
                    <td><input aria-label="Cantidad" type="number" min="0" step="1" required value={detail.cantidad} onChange={(event) => updateDetail(index, "cantidad", event.target.value)} /></td>
                    <td><input aria-label="Precio unitario" type="number" min="0" step="0.01" value={detail.precioUnitario ?? ""} onChange={(event) => updateDetail(index, "precioUnitario", event.target.value)} /></td>
                    <td><input aria-label="Bonificación" type="number" min="0" max="100" step="0.01" value={detail.porcentajeBonificacion ?? 0} onChange={(event) => updateDetail(index, "porcentajeBonificacion", event.target.value)} /></td>
                    <td><input aria-label="IVA" type="number" min="0" step="0.01" value={detail.porcentajeIva ?? 21} onChange={(event) => updateDetail(index, "porcentajeIva", event.target.value)} /></td>
                    <td><IconButton type="button" className="detail-delete-button" icon="ph-trash" label="Quitar ítem" onClick={() => removeDetail(index)} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
            {details.length > 0 && <div className="details-add-action"><button type="button" className="outlined-button" onClick={openArticleExplorer}><i className="ph ph-plus" aria-hidden="true" /><span>Agregar artículo</span></button></div>}
          </div>
          <div className="calculated-total"><span>Total calculado</span><output>{money(total)}</output></div>
          <footer><button type="submit" data-variant="primary" disabled={saving || !ready}><i className="ph ph-check" aria-hidden="true" /><span>{saving ? "Guardando…" : editing ? "Guardar cambios" : "Crear factura"}</span></button></footer>
        </form>
      )}
    </Modal>
    <ClientExplorerModal
      open={clientExplorerOpen}
      service={service}
      onClose={() => setClientExplorerOpen(false)}
      onSelect={(client) => {
        changeClient(client);
        setClientExplorerOpen(false);
      }}
    />
    <PriceArticleExplorerModal
      open={articleExplorerOpen}
      service={service}
      listaPrecioId={Number(header.listaPrecioId)}
      listaPrecioNombre={summaryPriceList}
      onClose={() => setArticleExplorerOpen(false)}
      onSelect={(article) => {
        addArticle(article);
        setArticleExplorerOpen(false);
      }}
    />
    </>
  );
}
