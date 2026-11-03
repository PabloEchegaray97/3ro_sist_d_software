import { useCallback, useEffect, useMemo, useState } from "react";
import { sileo } from "sileo";
import type { ArticuloAltaListaPrecioOpcionDTO, ArticuloDisponibleDTO } from "./models/factura.dto";
import type { ExplorerColumn } from "./PaginatedExplorerModal";
import { FacturaService } from "./services/FacturaService";
import { errorMessage, money } from "./formatters";
import { PaginatedDataView } from "./PaginatedDataView";
import { ArticleFormModal } from "./ArticleFormModal";
import { DeactivateEntityModal } from "./DeactivateEntityModal";
import { fullPriceListLabel, priceListTier, shortPriceListLabel } from "./price-list-labels";

export function ArticlesView({ service }: { service: FacturaService }) {
  const [formOpen, setFormOpen] = useState(false);
  const [editingArticleId, setEditingArticleId] = useState<number | null>(null);
  const [deletingArticle, setDeletingArticle] = useState<ArticuloDisponibleDTO | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [priceLists, setPriceLists] = useState<ArticuloAltaListaPrecioOpcionDTO[]>([]);
  const [selectedPriceListId, setSelectedPriceListId] = useState<number | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const columns = useMemo<ExplorerColumn<ArticuloDisponibleDTO>[]>(() => [
    { header: "Artículo ID", render: (item) => item.articuloId, searchValue: (item) => item.articuloId },
    { header: "Código", render: (item) => item.codigo, searchValue: (item) => item.codigo },
    { header: "Artículo", render: (item) => item.denominacion, searchValue: (item) => item.denominacion },
    { header: "Rubro", render: (item) => item.rubro, searchValue: (item) => item.rubro },
    { header: "Marca", render: (item) => item.marca ?? "—", searchValue: (item) => item.marca },
    { header: "Lista", render: (item) => <span className="price-list-name" data-price-tier={priceListTier(item.listaPrecioCodigo)}>{shortPriceListLabel(item.listaPrecioCodigo)}</span>, searchValue: (item) => item.listaPrecioCodigo },
    { header: "Precio final", render: (item) => money(item.precioVenta), searchValue: (item) => item.precioVenta, align: "right" },
  ], []);
  useEffect(() => {
    let active = true;
    service.obtenerOpcionesAltaArticulo().then((options) => {
      if (!active) return;
      setPriceLists(options.listasPrecio);
      const minorista = options.listasPrecio.find((list) =>
        list.codigo.trim().toUpperCase() === "MINORISTA"
        || list.denominacion.toLowerCase().includes("minorista")
      );
      setSelectedPriceListId(minorista?.id ?? options.listasPrecio[0]?.id ?? null);
    }).catch((error) => {
      if (active) sileo.error({ title: "No se pudieron cargar las listas de precios", description: errorMessage(error) });
    });
    return () => { active = false; };
  }, [service]);

  const loadPage = useCallback(
    (search: string, page: number, size: number) =>
      service.buscarArticulosDisponibles(search, page, size, selectedPriceListId),
    [selectedPriceListId, service],
  );

  const closeForm = () => {
    setFormOpen(false);
    setEditingArticleId(null);
  };
  const saved = () => {
    closeForm();
    setRefreshKey((value) => value + 1);
  };
  const confirmDelete = async () => {
    if (!deletingArticle || deleting) return;
    setDeleting(true);
    try {
      const options = await service.obtenerOpcionesFactura();
      if (!options.usuario) throw new Error("No hay un usuario disponible para registrar la baja");
      await service.desactivarArticulo(deletingArticle.articuloId, options.usuario.id);
      sileo.success({ title: "Artículo eliminado", description: `${deletingArticle.denominacion} fue marcado como inactivo.` });
      setDeletingArticle(null);
      setRefreshKey((value) => value + 1);
    } catch (error) {
      sileo.error({ title: "No se pudo eliminar el artículo", description: errorMessage(error) });
    } finally {
      setDeleting(false);
    }
  };
  return <>
    <PaginatedDataView title="Artículos" description="Consultá los artículos y sus precios por lista." searchPlaceholder="Buscar por código, artículo, rubro, marca o lista…" emptyText="No hay artículos para mostrar." columns={columns} getKey={(item) => item.listaPrecioArticuloId} getItemLabel={(item) => item.denominacion} loadPage={loadPage} actionLabel="Nuevo artículo" actionIcon="ph-plus" onAction={() => setFormOpen(true)} onEdit={(item) => setEditingArticleId(item.articuloId)} onDelete={(item) => setDeletingArticle(item)} refreshKey={refreshKey} tableClassName="articles-data-table" controls={<select className="explorer-order-select" aria-label="Lista de precios" value={selectedPriceListId ?? "ALL"} disabled={priceLists.length === 0} onChange={(event) => setSelectedPriceListId(event.target.value === "ALL" ? null : Number(event.target.value))}>{priceLists.length === 0 ? <option value="ALL">Cargando listas…</option> : <option value="ALL">Ver todos</option>}{priceLists.map((list) => <option key={list.id} value={list.id}>{fullPriceListLabel(list.codigo, list.denominacion)}</option>)}</select>} />
    <ArticleFormModal open={formOpen || editingArticleId != null} articleId={editingArticleId} service={service} onClose={closeForm} onSaved={saved} />
    <DeactivateEntityModal open={deletingArticle != null} entityType="artículo" entityName={deletingArticle?.denominacion ?? ""} busy={deleting} onClose={() => { if (!deleting) setDeletingArticle(null); }} onConfirm={() => void confirmDelete()} />
  </>;
}
