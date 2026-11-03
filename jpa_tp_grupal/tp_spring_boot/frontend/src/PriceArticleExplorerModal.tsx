import { useCallback, useMemo } from "react";
import type { ArticuloDisponibleDTO } from "./models/factura.dto";
import { money } from "./formatters";
import { PaginatedExplorerModal, type ExplorerColumn } from "./PaginatedExplorerModal";
import { FacturaService } from "./services/FacturaService";
import { priceListTier, shortPriceListLabel } from "./price-list-labels";

interface PriceArticleExplorerModalProps {
  open: boolean;
  service: FacturaService;
  listaPrecioId: number;
  listaPrecioNombre: string;
  onSelect: (article: ArticuloDisponibleDTO) => void;
  onClose: () => void;
}

export function PriceArticleExplorerModal({ open, service, listaPrecioId, listaPrecioNombre, onSelect, onClose }: PriceArticleExplorerModalProps) {
  const columns = useMemo<ExplorerColumn<ArticuloDisponibleDTO>[]>(() => [
    { header: "Precio ID", render: (article) => article.listaPrecioArticuloId, searchValue: (article) => article.listaPrecioArticuloId },
    { header: "Artículo ID", render: (article) => article.articuloId, searchValue: (article) => article.articuloId },
    { header: "Código", render: (article) => article.codigo, searchValue: (article) => article.codigo },
    { header: "Artículo", render: (article) => article.denominacion, searchValue: (article) => article.denominacion },
    { header: "Rubro", render: (article) => article.rubro, searchValue: (article) => `${article.rubroCodigo} ${article.rubro}` },
    { header: "Marca", render: (article) => article.marca ?? "—", searchValue: (article) => `${article.marcaCodigo ?? ""} ${article.marca ?? ""}` },
    { header: "Lista", render: (article) => <span className="price-list-name" data-price-tier={priceListTier(article.listaPrecioCodigo)}>{shortPriceListLabel(article.listaPrecioCodigo)}</span>, searchValue: (article) => `${article.listaPrecioId} ${article.listaPrecioCodigo}` },
    { header: "Precio", render: (article) => money(article.precioVenta), searchValue: (article) => article.precioVenta, align: "right" },
  ], []);
  const loadPage = useCallback(
    (search: string, page: number, pageSize: number) =>
      service.buscarArticulosDisponibles(search, page, pageSize, listaPrecioId),
    [listaPrecioId, service],
  );

  return (
    <PaginatedExplorerModal
      open={open}
      title={`Agregar artículo · ${listaPrecioNombre}`}
      searchPlaceholder="Escribí cualquier dato del artículo o de la lista…"
      columns={columns}
      loadPage={loadPage}
      getKey={(article) => article.listaPrecioArticuloId}
      emptyText="No hay artículos con precio que coincidan con la búsqueda."
      selectLabel="Agregar artículo"
      selectIcon="ph-plus"
      iconOnlyAction
      selectOnRowClick
      tableClassName="article-explorer-table"
      tableShellClassName="article-explorer-shell"
      requestDependency={listaPrecioId}
      onSelect={onSelect}
      onClose={onClose}
    />
  );
}
