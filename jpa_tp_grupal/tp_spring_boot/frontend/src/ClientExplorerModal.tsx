import { useCallback, useMemo, useState } from "react";
import type { ClienteDisponibleDTO } from "./models/factura.dto";
import { PaginatedExplorerModal, type ExplorerColumn } from "./PaginatedExplorerModal";
import { FacturaService } from "./services/FacturaService";
import { dateTimeParts } from "./formatters";

interface ClientExplorerModalProps {
  open: boolean;
  service: FacturaService;
  onSelect: (client: ClienteDisponibleDTO) => void;
  onClose: () => void;
}

const valueOrDash = (value: string | null) => value?.trim() || "—";
const dateOrDash = (value: string | null) => value ? dateTimeParts(value).date : "—";

export function ClientExplorerModal({ open, service, onSelect, onClose }: ClientExplorerModalProps) {
  const [order, setOrder] = useState("ULTIMOS_AGREGADOS");
  const columns = useMemo<ExplorerColumn<ClienteDisponibleDTO>[]>(() => [
    { header: "ID", render: (client) => client.id, searchValue: (client) => client.id },
    { header: "CUIT/CUIL", render: (client) => <>{client.cuitCuil}<small>{valueOrDash(client.condicionIva)}</small><small>Lista: {client.listaPrecio ?? "Sin asignar"}</small></>, searchValue: (client) => `${client.cuitCuil} ${client.condicionIvaId ?? ""} ${client.condicionIva ?? ""} ${client.listaPrecioId ?? ""} ${client.listaPrecioCodigo ?? ""} ${client.listaPrecio ?? ""}` },
    { header: "Denominación", render: (client) => <>{client.denominacion}<small>Alta el {dateOrDash(client.fechaAlta)}</small></>, searchValue: (client) => `${client.denominacion} ${client.fechaAlta}` },
    { header: "Contacto", render: (client) => <>{valueOrDash(client.telefono)}{client.celular && <small>Cel. {client.celular}</small>}{client.email && <small>{client.email}</small>}</>, searchValue: (client) => `${client.telefono ?? ""} ${client.celular ?? ""} ${client.email ?? ""}` },
    { header: "Domicilio", render: (client) => valueOrDash([client.nombreCalle, client.numeroCalle].filter(Boolean).join(" ")), searchValue: (client) => `${client.nombreCalle ?? ""} ${client.numeroCalle ?? ""}` },
    { header: "Última compra", render: (client) => dateOrDash(client.ultimaCompra), searchValue: (client) => client.ultimaCompra },
  ], []);
  const loadPage = useCallback(
    (search: string, page: number, pageSize: number) =>
      service.buscarClientesDisponibles(search, page, pageSize, order),
    [order, service],
  );

  return (
    <PaginatedExplorerModal
      open={open}
      title="Buscar cliente"
      searchPlaceholder="Escribí cualquier dato del cliente…"
      columns={columns}
      loadPage={loadPage}
      getKey={(client) => client.id}
      emptyText="No hay clientes que coincidan con la búsqueda."
      selectLabel="Elegir cliente"
      selectIcon="ph-arrow-right"
      iconOnlyAction
      selectOnRowClick
      requestDependency={order}
      tableClassName="client-explorer-table"
      tableShellClassName="client-explorer-shell"
      controls={(
        <select className="explorer-order-select" aria-label="Ordenar clientes" value={order} onChange={(event) => setOrder(event.target.value)}>
          <option value="ULTIMOS_AGREGADOS">Últimos agregados</option>
          <option value="COMPRADOR_RECIENTE">Comprador reciente</option>
        </select>
      )}
      onSelect={onSelect}
      onClose={onClose}
    />
  );
}
