import { useCallback, useState, type MouseEvent } from "react";
import type { FacturaReporteDTO } from "./models/factura.dto";
import type { InvoiceAction } from "./invoice-actions";
import { dateTimeParts, formatStatus, money } from "./formatters";
import { IconButton } from "./IconButton";
import { InvoiceContextMenu } from "./InvoiceContextMenu";

interface MenuState {
  invoice: FacturaReporteDTO;
  x: number;
  y: number;
}

interface InvoiceTableProps {
  invoices: FacturaReporteDTO[];
  loading: boolean;
  dateMode: "creation" | "issuance";
  onAction: (action: InvoiceAction, invoice: FacturaReporteDTO) => void;
}

export function InvoiceTable({ invoices, loading, dateMode, onAction }: InvoiceTableProps) {
  const [menu, setMenu] = useState<MenuState | null>(null);
  const closeMenu = useCallback(() => setMenu(null), []);

  const openAt = (invoice: FacturaReporteDTO, x: number, y: number) => {
    const menuWidth = 190;
    const status = invoice.estado.trim().toUpperCase();
    const menuHeight = status === "SIN_EMITIR" ? 184 : status === "ANULADA" ? 146 : 108;
    setMenu({
      invoice,
      x: Math.max(8, Math.min(x, window.innerWidth - menuWidth - 8)),
      y: Math.max(8, Math.min(y, window.innerHeight - menuHeight - 8)),
    });
  };

  const openFromRow = (event: MouseEvent<HTMLTableRowElement>, invoice: FacturaReporteDTO) => {
    event.preventDefault();
    openAt(invoice, event.clientX, event.clientY);
  };

  const openFromButton = (event: MouseEvent<HTMLButtonElement>, invoice: FacturaReporteDTO) => {
    event.stopPropagation();
    const rect = event.currentTarget.getBoundingClientRect();
    openAt(invoice, rect.right - 180, rect.bottom + 4);
  };

  return (
    <div className="table-shell">
      <table className="facturas-table">
        <thead>
          <tr>
            <th>ID</th><th>Número</th><th>{dateMode === "creation" ? "Creación interna" : "Emisión"}</th><th>Cliente</th><th>Condición IVA</th>
            <th>Punto de venta</th><th>Total</th><th>Ítems</th><th>Estado</th>
            <th className="row-menu-column"><span className="sr-only">Menú</span></th>
          </tr>
        </thead>
        <tbody>
          {loading && <tr><td colSpan={10} data-empty>Cargando comprobantes…</td></tr>}
          {!loading && invoices.length === 0 && (
            <tr><td colSpan={10} data-empty>No hay facturas para los filtros seleccionados.</td></tr>
          )}
          {!loading && invoices.map((invoice) => {
            const status = invoice.estado.trim().toUpperCase();
            const date = dateTimeParts(
              dateMode === "creation" ? invoice.fechaCreacionInterna : invoice.fechaEmision,
            );
            return (
              <tr
                key={invoice.id}
                tabIndex={0}
                onClick={() => onAction("view", invoice)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    onAction("view", invoice);
                  }
                }}
                onContextMenu={(event) => openFromRow(event, invoice)}
                aria-label={`Ver factura ${invoice.numeroFactura ?? `borrador ${invoice.id}`}. Clic secundario para abrir acciones.`}
              >
                <td>{invoice.id}</td>
                <td>{invoice.numeroFactura ?? "—"}</td>
                <td><span className="date-time"><span>{date.date}</span><small>{date.time}</small></span></td>
                <td>{invoice.clienteDenominacion}</td>
                <td>{invoice.condicionIva}</td>
                <td>{invoice.puntoVentaDescripcion}</td>
                <td data-money>{money(invoice.importeTotal)}</td>
                <td>{invoice.cantidadItems}</td>
                <td data-status={status === "SIN_EMITIR" ? "pendiente" : status === "ANULADA" ? "anulada" : "emitida"}>{formatStatus(invoice.estado).replace("_", " ")}</td>
                <td className="row-menu-column">
                  <IconButton
                    type="button"
                    icon="ph-dots-three-vertical"
                    label={`Acciones de la factura ${invoice.numeroFactura ?? `borrador ${invoice.id}`}`}
                    aria-haspopup="menu"
                    aria-expanded={menu?.invoice.id === invoice.id}
                    onClick={(event) => openFromButton(event, invoice)}
                  />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {menu && (
        <InvoiceContextMenu
          invoice={menu.invoice}
          x={menu.x}
          y={menu.y}
          onAction={onAction}
          onClose={closeMenu}
        />
      )}
    </div>
  );
}
