import "@phosphor-icons/web/regular";
import "./styles.css";
import type {
  ArticuloDisponibleDTO,
  FacturaDetalleRequestDTO,
  FacturaFiltrosDTO,
  FacturaOpcionesDTO,
  FacturaReporteDTO,
  FacturaRequestDTO,
  FacturaVistaDTO,
  OpcionDTO,
} from "./models/factura.dto";
import { FacturaService } from "./services/FacturaService";

// CONSIGNA ESCRITURA - 7.b: estado, DOM y eventos sin framework visual.
const service = new FacturaService();
let detalles: FacturaDetalleRequestDTO[] = [];
let facturas: FacturaReporteDTO[] = [];
let articulosDisponibles: ArticuloDisponibleDTO[] = [];
let opcionesFacturaCargadas = false;
let opcionesFactura: FacturaOpcionesDTO | null = null;
let accionPendiente: { tipo: "anular" | "eliminar"; factura: FacturaReporteDTO } | null = null;

function byId<T extends HTMLElement>(id: string): T {
  const node = document.getElementById(id);
  if (!node) throw new Error(`No se encontró #${id}`);
  return node as T;
}

const value = (id: string) => byId<HTMLInputElement>(id).value;
const numberValue = (id: string) => Number(value(id));
const optionalNumber = (id: string) => value(id) === "" ? null : numberValue(id);

function detailFromArticle(article: ArticuloDisponibleDTO): FacturaDetalleRequestDTO {
  return {
    articuloId: article.articuloId,
    listaPrecioArticuloId: article.listaPrecioArticuloId,
    descripcion: article.denominacion,
    cantidad: 1,
    precioUnitario: article.precioVenta,
    porcentajeBonificacion: 0,
    porcentajeIva: 21,
  };
}

function renderDetails(): void {
  const body = byId<HTMLTableSectionElement>("detalles-body");
  if (detalles.length === 0) {
    body.innerHTML = '<tr><td colspan="8" data-empty>Todavía no agregaste ítems.</td></tr>';
    renderTotal();
    return;
  }
  body.innerHTML = detalles.map((detail, index) => `
    <tr data-index="${index}">
      <td><input data-field="articuloId" aria-label="Artículo ID" type="number" min="1" value="${detail.articuloId || ""}" required></td>
      <td><input data-field="listaPrecioArticuloId" aria-label="Precio lista ID" type="number" min="1" value="${detail.listaPrecioArticuloId || ""}" required></td>
      <td><input data-field="descripcion" aria-label="Descripción" value="${escapeHtml(detail.descripcion ?? "")}" placeholder="Usar artículo"></td>
      <td><input data-field="cantidad" aria-label="Cantidad" type="number" min="0.01" step="0.01" value="${detail.cantidad}" required></td>
      <td><input data-field="precioUnitario" aria-label="Precio unitario" type="number" min="0" step="0.01" value="${detail.precioUnitario ?? ""}" placeholder="Precio de lista"></td>
      <td><input data-field="porcentajeBonificacion" aria-label="Bonificación" type="number" min="0" max="100" step="0.01" value="${detail.porcentajeBonificacion ?? 0}"></td>
      <td><input data-field="porcentajeIva" aria-label="IVA" type="number" min="0" step="0.01" value="${detail.porcentajeIva ?? 21}"></td>
      <td><button type="button" data-action="quitar" data-index="${index}"><i class="ph ph-trash" aria-hidden="true"></i><span>Quitar</span></button></td>
    </tr>
  `).join("");

  body.querySelectorAll<HTMLInputElement>("input").forEach((input) => {
    input.addEventListener("input", () => {
      const row = input.closest<HTMLTableRowElement>("tr");
      if (!row) return;
      const index = Number(row.dataset.index);
      const field = input.dataset.field as keyof FacturaDetalleRequestDTO;
      const numeric = field !== "descripcion";
      (detalles[index] as unknown as Record<string, string | number | null>)[field] = numeric
        ? (input.value === "" ? null : Number(input.value))
        : (input.value || null);
      renderTotal();
    });
  });

  body.querySelectorAll<HTMLButtonElement>('[data-action="quitar"]').forEach((button) => {
    button.addEventListener("click", () => {
      detalles.splice(Number(button.dataset.index), 1);
      renderDetails();
    });
  });
  renderTotal();
}

function calculatedTotal(): number {
  const total = detalles.reduce((accumulator, detail) => {
    const listPrice = articulosDisponibles.find(
      (article) => article.listaPrecioArticuloId === detail.listaPrecioArticuloId,
    )?.precioVenta ?? 0;
    const unitPrice = detail.precioUnitario ?? listPrice;
    const discount = detail.porcentajeBonificacion ?? 0;
    const iva = detail.porcentajeIva ?? 21;
    const gross = detail.cantidad * unitPrice;
    const net = roundMoney(gross - (gross * discount / 100));
    const ivaAmount = roundMoney(net * iva / 100);
    return accumulator + roundMoney(net + ivaAmount);
  }, 0);
  return roundMoney(total);
}

function renderTotal(): void {
  byId<HTMLOutputElement>("importe-total").value = money(calculatedTotal());
}

function fillSelect(id: string, options: OpcionDTO[], emptyText: string, allowEmpty = false): void {
  const select = byId<HTMLSelectElement>(id);
  const emptyOption = allowEmpty ? `<option value="">${emptyText}</option>` : "";
  select.innerHTML = emptyOption + options.map((option) =>
    `<option value="${option.id}">${escapeHtml(option.descripcion)}</option>`
  ).join("");
  if (options.length === 0 && !allowEmpty) {
    select.innerHTML = `<option value="">${emptyText}</option>`;
  }
  select.disabled = options.length === 0;
}

async function loadInvoiceOptions(): Promise<void> {
  opcionesFacturaCargadas = false;
  try {
    const options: FacturaOpcionesDTO = await service.obtenerOpcionesFactura();
    opcionesFactura = options;
    byId<HTMLInputElement>("numero").value = String(options.siguienteNumero);
    fillSelect("punto-venta-id", options.puntosVenta, "No hay puntos de venta");
    fillSelect("cliente-id", options.clientes, "Consumidor final", true);
    fillSelect("condicion-iva-id", options.condicionesIva, "No hay condiciones de IVA");
    fillSelect("tipo-moneda-id", options.tiposMoneda, "No hay tipos de moneda");

    const userSelect = byId<HTMLSelectElement>("usuario-id");
    userSelect.innerHTML = options.usuario
      ? `<option value="${options.usuario.id}">${escapeHtml(options.usuario.descripcion)}</option>`
      : '<option value="">No existe el usuario admin</option>';
    userSelect.disabled = true;
    syncClientTaxCondition();

    opcionesFacturaCargadas = Boolean(
      options.usuario
      && options.puntosVenta.length
      && options.condicionesIva.length
      && options.tiposMoneda.length,
    );
  } catch (error) {
    opcionesFactura = null;
    showMessage(errorMessage(error), true);
  }
}

function syncClientTaxCondition(): void {
  const conditionSelect = byId<HTMLSelectElement>("condicion-iva-id");
  const clientId = optionalNumber("cliente-id");
  const client = opcionesFactura?.clientes.find((item) => item.id === clientId);
  const consumidorFinal = opcionesFactura?.condicionesIva.find((condition) =>
    condition.descripcion.toLowerCase().includes("consumidor final")
  );
  const conditionId = clientId == null
    ? consumidorFinal?.id
    : client?.condicionIvaId;

  if (conditionId != null) conditionSelect.value = String(conditionId);
  conditionSelect.disabled = true;
}

async function loadAvailableArticles(): Promise<void> {
  const select = byId<HTMLSelectElement>("articulo-disponible");
  const addButton = byId<HTMLButtonElement>("agregar-detalle");
  try {
    articulosDisponibles = await service.obtenerArticulosDisponibles();
    if (articulosDisponibles.length === 0) {
      select.innerHTML = "<option>No hay artículos con precio disponibles</option>";
      select.disabled = true;
      addButton.disabled = true;
      return;
    }
    select.innerHTML = articulosDisponibles.map((article) => `
      <option value="${article.listaPrecioArticuloId}">${escapeHtml(article.codigo)} — ${escapeHtml(article.denominacion)} — ${escapeHtml(article.listaPrecio)} — ${money(article.precioVenta)}</option>
    `).join("");
    select.disabled = false;
    addButton.disabled = false;
  } catch (error) {
    select.innerHTML = "<option>No se pudieron cargar los artículos</option>";
    select.disabled = true;
    addButton.disabled = true;
    showMessage(errorMessage(error), true);
  }
}

function addSelectedArticle(): void {
  const selectedId = numberValue("articulo-disponible");
  const article = articulosDisponibles.find((item) => item.listaPrecioArticuloId === selectedId);
  if (!article) return;

  const existing = detalles.find((detail) => detail.listaPrecioArticuloId === selectedId);
  if (existing) {
    existing.cantidad += 1;
  } else {
    detalles.push(detailFromArticle(article));
  }
  renderDetails();
}

async function loadInvoices(): Promise<void> {
  const body = byId<HTMLTableSectionElement>("facturas-body");
  body.innerHTML = '<tr><td colspan="10" data-empty>Cargando comprobantes…</td></tr>';
  try {
    facturas = await service.obtenerFacturas(currentFilters());
    renderInvoices();
  } catch (error) {
    body.innerHTML = '<tr><td colspan="10" data-empty>No se pudo cargar la información.</td></tr>';
    showMessage(errorMessage(error), true);
  }
}

function renderInvoices(): void {
  const body = byId<HTMLTableSectionElement>("facturas-body");
  if (facturas.length === 0) {
    body.innerHTML = '<tr><td colspan="10" data-empty>No hay facturas para los filtros seleccionados.</td></tr>';
    return;
  }
  body.innerHTML = facturas.map((invoice) => {
    const status = invoice.estado.trim();
    const annulled = status.toUpperCase() === "ANULADA";
    const annulAction = annulled
      ? ""
      : `<button type="button" data-action="anular" data-id="${invoice.id}"><i class="ph ph-prohibit" aria-hidden="true"></i><span>Anular</span></button>`;
    return `
    <tr>
      <td>${invoice.id}</td><td>${invoice.numeroFactura}</td><td>${dateTimeHtml(invoice.fechaEmision)}</td>
      <td>${escapeHtml(invoice.clienteDenominacion)}</td><td>${escapeHtml(invoice.condicionIva)}</td>
      <td>${escapeHtml(invoice.puntoVentaDescripcion)}</td><td data-money>${money(invoice.importeTotal)}</td>
      <td>${invoice.cantidadItems}</td><td data-status="${annulled ? "anulada" : "emitida"}">${formatStatus(status)}</td>
      <td><div data-actions><button type="button" data-action="ver" data-id="${invoice.id}"><i class="ph ph-eye" aria-hidden="true"></i><span>Ver factura</span></button>${annulAction}<button type="button" data-action="eliminar" data-id="${invoice.id}"><i class="ph ph-trash" aria-hidden="true"></i><span>Eliminar</span></button></div></td>
    </tr>
  `;
  }).join("");

  body.querySelectorAll<HTMLButtonElement>("[data-action]").forEach((button) => {
    button.addEventListener("click", () => {
      const invoice = facturas.find((item) => item.id === Number(button.dataset.id));
      if (!invoice) return;
      if (button.dataset.action === "ver") {
        void openInvoiceView(invoice.id);
        return;
      }
      openConfirmation(button.dataset.action as "anular" | "eliminar", invoice);
    });
  });
}

async function openInvoiceView(id: number): Promise<void> {
  const dialog = byId<HTMLDialogElement>("factura-vista-modal");
  const content = byId("factura-vista-contenido");
  byId("factura-vista-titulo").textContent = "Detalle de factura";
  content.innerHTML = '<p data-empty>Cargando factura…</p>';
  dialog.showModal();
  try {
    const invoice = await service.obtenerFactura(id);
    byId("factura-vista-titulo").textContent = `Factura ${invoice.numero}`;
    content.innerHTML = invoiceViewHtml(invoice);
  } catch (error) {
    content.innerHTML = '<p data-empty>No se pudo cargar la factura.</p>';
    showMessage(errorMessage(error), true);
  }
}

function invoiceViewHtml(invoice: FacturaVistaDTO): string {
  const cancelledDate = invoice.fechaAnulacion
    ? `<div><dt>Fecha de anulación</dt><dd>${escapeHtml(invoice.fechaAnulacion)}</dd></div>`
    : "";
  const rows = invoice.detalles.map((detail) => `
    <tr>
      <td>${escapeHtml(detail.articuloCodigo)}<small>${escapeHtml(detail.articuloDenominacion)} · ID ${detail.articuloId}</small></td>
      <td>${escapeHtml(detail.listaPrecio)}<small>ID ${detail.listaPrecioArticuloId}</small></td>
      <td>${escapeHtml(detail.descripcion ?? "")}</td>
      <td>${decimal(detail.cantidad)}</td>
      <td data-money>${money(detail.precioUnitario)}</td>
      <td>${decimal(detail.porcentajeBonificacion)} %</td>
      <td>${decimal(detail.porcentajeIva)} %</td>
      <td data-money>${money(detail.importeNeto)}</td>
      <td data-money>${money(detail.importeIva)}</td>
      <td data-money>${money(detail.importeSubtotal)}</td>
    </tr>
  `).join("");

  return `
    <dl data-invoice-summary>
      <div><dt>Número</dt><dd>${invoice.numero}</dd></div>
      <div><dt>Fecha</dt><dd>${dateTimeHtml(invoice.fechaEmision)}</dd></div>
      <div><dt>Estado</dt><dd data-status="${invoice.estado.toUpperCase() === "ANULADA" ? "anulada" : "emitida"}">${formatStatus(invoice.estado)}</dd></div>
      <div><dt>Punto de venta</dt><dd>${escapeHtml(invoice.puntoVentaDescripcion)} · ID ${invoice.puntoVentaId}</dd></div>
      <div><dt>Cliente</dt><dd>${escapeHtml(invoice.clienteDenominacion)}${invoice.clienteId == null ? "" : ` · ID ${invoice.clienteId}`}</dd></div>
      <div><dt>Condición de IVA</dt><dd>${escapeHtml(invoice.condicionIva)} · ID ${invoice.condicionIvaId}</dd></div>
      <div><dt>Moneda</dt><dd>${escapeHtml(invoice.tipoMoneda)} · ID ${invoice.tipoMonedaId}</dd></div>
      <div><dt>Usuario</dt><dd>${escapeHtml(invoice.usuario)} · ID ${invoice.usuarioId}</dd></div>
      ${cancelledDate}
    </dl>
    <div data-invoice-notes><span>Observaciones</span><p>${escapeHtml(invoice.observaciones || "Sin observaciones")}</p></div>
    <div data-table>
      <table data-invoice-details>
        <thead><tr><th>Artículo</th><th>Lista</th><th>Descripción</th><th>Cantidad</th><th>Precio</th><th>Bonif.</th><th>IVA</th><th>Neto</th><th>IVA importe</th><th>Subtotal</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
    <dl data-invoice-totals>
      <div><dt>Total</dt><dd>${money(invoice.importeTotal)}</dd></div>
      <div><dt>Cobrado</dt><dd>${money(invoice.importeCobrado)}</dd></div>
      <div><dt>Saldo</dt><dd>${money(invoice.importeSaldo)}</dd></div>
    </dl>
  `;
}

function openConfirmation(tipo: "anular" | "eliminar", factura: FacturaReporteDTO): void {
  accionPendiente = { tipo, factura };
  byId("confirmacion-titulo").textContent = tipo === "anular" ? "Anular factura" : "Eliminar factura";
  byId("confirmacion-texto").textContent = tipo === "anular"
    ? `La factura ${factura.numeroFactura} quedará anulada.`
    : `La factura ${factura.numeroFactura} y sus detalles se eliminarán definitivamente.`;
  byId("confirmar-accion").dataset.danger = String(tipo === "eliminar");
  byId<HTMLDialogElement>("confirmacion").showModal();
}

async function confirmAction(): Promise<void> {
  if (!accionPendiente) return;
  const { tipo, factura } = accionPendiente;
  byId<HTMLDialogElement>("confirmacion").close();
  accionPendiente = null;
  try {
    if (tipo === "anular") {
      await service.actualizarEstado(factura.id, "ANULADA", "Anulada desde el frontend");
      showMessage(`Factura ${factura.numeroFactura} anulada`);
    } else {
      await service.eliminarFactura(factura.id);
      showMessage(`Factura ${factura.numeroFactura} eliminada`);
    }
    await loadInvoices();
  } catch (error) {
    showMessage(errorMessage(error), true);
  }
}

async function createInvoice(event: Event): Promise<void> {
  event.preventDefault();
  if (!opcionesFacturaCargadas) {
    showMessage("Faltan datos maestros obligatorios para crear la factura", true);
    return;
  }
  if (detalles.length === 0) {
    showMessage("Agregá al menos un ítem a la factura", true);
    return;
  }
  const request: FacturaRequestDTO = {
    numero: numberValue("numero"),
    fechaEmision: value("fecha-emision"),
    puntoVentaId: numberValue("punto-venta-id"),
    clienteId: optionalNumber("cliente-id"),
    condicionIvaId: numberValue("condicion-iva-id"),
    tipoMonedaId: numberValue("tipo-moneda-id"),
    usuarioId: numberValue("usuario-id"),
    estado: "EMITIDA",
    importeCobrado: calculatedTotal(),
    observaciones: value("observaciones") || null,
    detalles,
  };

  try {
    const created = await service.crearFactura(request);
    showMessage(`Factura ${created.numeroFactura} creada correctamente`);
    byId<HTMLFormElement>("form-factura").reset();
    byId<HTMLInputElement>("fecha-emision").value = localDateTime();
    detalles = [];
    renderDetails();
    byId<HTMLDialogElement>("nueva-factura-modal").close();
    await Promise.all([loadInvoiceOptions(), loadInvoices()]);
  } catch (error) {
    showMessage(errorMessage(error), true);
  }
}

function currentFilters(): FacturaFiltrosDTO {
  return {
    fechaDesde: value("fecha-desde") || undefined,
    fechaHasta: value("fecha-hasta") || undefined,
    estado: value("filtro-estado") || undefined,
    montoMinimo: optionalNumber("monto-minimo") ?? undefined,
  };
}

function showMessage(text: string, isError = false): void {
  const output = byId("mensaje");
  output.textContent = text;
  output.dataset.visible = "true";
  output.dataset.type = isError ? "error" : "success";
  window.setTimeout(() => output.dataset.visible = "false", 4200);
}

const money = (amount: number) => new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(amount);
const decimal = (amount: number) => new Intl.NumberFormat("es-AR", { maximumFractionDigits: 2 }).format(amount);
const roundMoney = (amount: number) => Math.round(amount * 100) / 100;
const formatStatus = (status: string) => status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
const errorMessage = (error: unknown) => error instanceof Error ? error.message : "Ocurrió un error";
const escapeHtml = (text: string) => text.replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[character] ?? character));
const localDateTime = () => {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
};

function dateTimeHtml(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return `<span>${escapeHtml(value)}</span>`;
  const formattedDate = new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
  const formattedTime = new Intl.DateTimeFormat("es-AR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date);
  return `<span data-date-time><span>${formattedDate}</span><small>${formattedTime}</small></span>`;
}

function enableDatePickers(): void {
  document.querySelectorAll<HTMLInputElement>('input[type="date"], input[type="datetime-local"]').forEach((input) => {
    input.addEventListener("click", () => {
      try {
        input.showPicker();
      } catch {
        input.focus();
      }
    });
  });
}

byId("recargar").addEventListener("click", () => void loadInvoices());
byId("abrir-nueva-factura").addEventListener("click", () => {
  byId<HTMLDialogElement>("nueva-factura-modal").showModal();
  void loadInvoiceOptions();
});
byId("cerrar-nueva-factura").addEventListener("click", () => byId<HTMLDialogElement>("nueva-factura-modal").close());
byId("abrir-filtros").addEventListener("click", () => byId<HTMLDialogElement>("filtros-modal").showModal());
byId("cerrar-filtros").addEventListener("click", () => byId<HTMLDialogElement>("filtros-modal").close());
byId("cerrar-factura-vista").addEventListener("click", () => byId<HTMLDialogElement>("factura-vista-modal").close());
byId("agregar-detalle").addEventListener("click", addSelectedArticle);
byId("cliente-id").addEventListener("change", syncClientTaxCondition);
byId<HTMLFormElement>("form-factura").addEventListener("submit", (event) => void createInvoice(event));
byId<HTMLFormElement>("form-filtros").addEventListener("submit", (event) => { event.preventDefault(); byId<HTMLDialogElement>("filtros-modal").close(); void loadInvoices(); });
byId("limpiar-filtros").addEventListener("click", () => { byId<HTMLFormElement>("form-filtros").reset(); byId<HTMLDialogElement>("filtros-modal").close(); void loadInvoices(); });
byId("cancelar-accion").addEventListener("click", () => { accionPendiente = null; byId<HTMLDialogElement>("confirmacion").close(); });
byId("confirmar-accion").addEventListener("click", () => void confirmAction());

byId<HTMLInputElement>("fecha-emision").value = localDateTime();
enableDatePickers();
detalles = [];
renderDetails();
void loadAvailableArticles();
void loadInvoiceOptions();
void loadInvoices();
