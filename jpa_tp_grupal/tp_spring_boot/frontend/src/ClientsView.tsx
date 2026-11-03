import { useCallback, useMemo, useState } from "react";
import { sileo } from "sileo";
import type { ClienteDisponibleDTO } from "./models/factura.dto";
import type { ExplorerColumn } from "./PaginatedExplorerModal";
import { FacturaService } from "./services/FacturaService";
import { dateTimeParts, errorMessage } from "./formatters";
import { PaginatedDataView } from "./PaginatedDataView";
import { ClientFormModal } from "./ClientFormModal";
import { DeactivateEntityModal } from "./DeactivateEntityModal";

const valueOrDash = (value: string | null) => value?.trim() || "—";
const dateOrDash = (value: string | null) => value ? dateTimeParts(value).date : "—";

export function ClientsView({ service }: { service: FacturaService }) {
  const [order, setOrder] = useState("ULTIMOS_AGREGADOS");
  const [formOpen, setFormOpen] = useState(false);
  const [editingClient, setEditingClient] = useState<ClienteDisponibleDTO | null>(null);
  const [deletingClient, setDeletingClient] = useState<ClienteDisponibleDTO | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const columns = useMemo<ExplorerColumn<ClienteDisponibleDTO>[]>(() => [
    { header: "ID", render: (item) => item.id, searchValue: (item) => item.id },
    { header: "CUIT/CUIL", render: (item) => <>{item.cuitCuil}<small>{valueOrDash(item.condicionIva)}</small><small>Lista: {item.listaPrecio ?? "Sin asignar"}</small></>, searchValue: (item) => item.cuitCuil },
    { header: "Denominación", render: (item) => <>{item.denominacion}<small>Alta el {dateOrDash(item.fechaAlta)}</small></>, searchValue: (item) => item.denominacion },
    { header: "Contacto", render: (item) => <>{valueOrDash(item.telefono)}{item.celular && <small>Cel. {item.celular}</small>}{item.email && <small>{item.email}</small>}</>, searchValue: (item) => item.email },
    { header: "Domicilio", render: (item) => valueOrDash([item.nombreCalle, item.numeroCalle].filter(Boolean).join(" ")), searchValue: (item) => item.nombreCalle },
    { header: "Última compra", render: (item) => dateOrDash(item.ultimaCompra), searchValue: (item) => item.ultimaCompra },
  ], []);
  const loadPage = useCallback((search: string, page: number, size: number) => service.buscarClientesDisponibles(search, page, size, order), [order, service]);

  const closeForm = () => {
    setFormOpen(false);
    setEditingClient(null);
  };
  const saved = () => {
    closeForm();
    setRefreshKey((value) => value + 1);
  };
  const confirmDelete = async () => {
    if (!deletingClient || deleting) return;
    setDeleting(true);
    try {
      const options = await service.obtenerOpcionesFactura();
      if (!options.usuario) throw new Error("No hay un usuario disponible para registrar la baja");
      await service.desactivarCliente(deletingClient.id, options.usuario.id);
      sileo.success({ title: "Cliente eliminado", description: `${deletingClient.denominacion} fue marcado como inactivo.` });
      setDeletingClient(null);
      setRefreshKey((value) => value + 1);
    } catch (error) {
      sileo.error({ title: "No se pudo eliminar el cliente", description: errorMessage(error) });
    } finally {
      setDeleting(false);
    }
  };

  return <>
    <PaginatedDataView title="Clientes" description="Consultá y administrá los clientes disponibles para facturación." searchPlaceholder="Buscar por cualquier dato del cliente…" emptyText="No hay clientes para mostrar." columns={columns} getKey={(item) => item.id} getItemLabel={(item) => item.denominacion} loadPage={loadPage} actionLabel="Nuevo cliente" actionIcon="ph-plus" onAction={() => setFormOpen(true)} onEdit={(item) => setEditingClient(item)} onDelete={(item) => setDeletingClient(item)} refreshKey={refreshKey} tableClassName="clients-data-table" controls={<select className="explorer-order-select" aria-label="Ordenar clientes" value={order} onChange={(e) => setOrder(e.target.value)}><option value="ULTIMOS_AGREGADOS">Últimos agregados</option><option value="COMPRADOR_RECIENTE">Comprador reciente</option></select>} />
    <ClientFormModal open={formOpen || editingClient != null} client={editingClient} service={service} onClose={closeForm} onSaved={saved} />
    <DeactivateEntityModal open={deletingClient != null} entityType="cliente" entityName={deletingClient?.denominacion ?? ""} busy={deleting} onClose={() => { if (!deleting) setDeletingClient(null); }} onConfirm={() => void confirmDelete()} />
  </>;
}
