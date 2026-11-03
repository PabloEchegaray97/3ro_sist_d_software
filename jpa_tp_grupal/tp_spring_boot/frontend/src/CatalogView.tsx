import { useCallback, useMemo, useState } from "react";
import { sileo } from "sileo";
import type { CatalogoItemDTO } from "./models/factura.dto";
import type { ExplorerColumn } from "./PaginatedExplorerModal";
import { dateTimeParts, errorMessage } from "./formatters";
import { PaginatedDataView } from "./PaginatedDataView";
import { CatalogFormModal, type CatalogKind } from "./CatalogFormModal";
import { DeactivateEntityModal } from "./DeactivateEntityModal";
import { FacturaService } from "./services/FacturaService";

const labels = {
  rubros: { title: "Rubros", singular: "rubro", description: "Administrá los rubros disponibles para clasificar artículos." },
  marcas: { title: "Marcas", singular: "marca", description: "Administrá las marcas disponibles para los artículos." },
} as const;

export function CatalogView({ kind, service }: { kind: CatalogKind; service: FacturaService }) {
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<CatalogoItemDTO | null>(null);
  const [deleting, setDeleting] = useState<CatalogoItemDTO | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const label = labels[kind];
  const columns = useMemo<ExplorerColumn<CatalogoItemDTO>[]>(() => [
    { header: "ID", render: (item) => item.id, searchValue: (item) => item.id },
    { header: "Código", render: (item) => item.codigo, searchValue: (item) => item.codigo },
    { header: "Denominación", render: (item) => item.denominacion, searchValue: (item) => item.denominacion },
    { header: "Fecha de alta", render: (item) => dateTimeParts(item.fechaAlta).date, searchValue: (item) => item.fechaAlta },
  ], []);
  const loadPage = useCallback(
    (search: string, page: number, size: number) => kind === "rubros" ? service.buscarRubros(search, page, size) : service.buscarMarcas(search, page, size),
    [kind, service],
  );

  const closeForm = () => { setFormOpen(false); setEditing(null); };
  const saved = () => { closeForm(); setRefreshKey((value) => value + 1); };
  const confirmDelete = async () => {
    if (!deleting || deleteBusy) return;
    setDeleteBusy(true);
    try {
      const options = await service.obtenerOpcionesFactura();
      if (!options.usuario) throw new Error("No hay un usuario disponible para registrar la baja");
      if (kind === "rubros") await service.desactivarRubro(deleting.id, options.usuario.id);
      else await service.desactivarMarca(deleting.id, options.usuario.id);
      sileo.success({ title: `${kind === "rubros" ? "Rubro" : "Marca"} eliminado`, description: `${deleting.denominacion} fue marcado como inactivo.` });
      setDeleting(null);
      setRefreshKey((value) => value + 1);
    } catch (error) {
      sileo.error({ title: `No se pudo eliminar el ${label.singular}`, description: errorMessage(error) });
    } finally {
      setDeleteBusy(false);
    }
  };

  return <>
    <PaginatedDataView
      title={label.title}
      description={label.description}
      searchPlaceholder={`Buscar ${label.singular} por ID, código o denominación…`}
      emptyText={`No hay ${label.title.toLowerCase()} para mostrar.`}
      columns={columns}
      getKey={(item) => item.id}
      getItemLabel={(item) => item.denominacion}
      loadPage={loadPage}
      actionLabel={`Nuevo ${label.singular}`}
      actionIcon="ph-plus"
      onAction={() => setFormOpen(true)}
      onEdit={setEditing}
      onDelete={setDeleting}
      refreshKey={refreshKey}
      tableClassName="catalog-data-table"
    />
    <CatalogFormModal open={formOpen || editing != null} kind={kind} item={editing} service={service} onClose={closeForm} onSaved={saved} />
    <DeactivateEntityModal open={deleting != null} entityType={label.singular} entityName={deleting?.denominacion ?? ""} busy={deleteBusy} onClose={() => { if (!deleteBusy) setDeleting(null); }} onConfirm={() => void confirmDelete()} />
  </>;
}
