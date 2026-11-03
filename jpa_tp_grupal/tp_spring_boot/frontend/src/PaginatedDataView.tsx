import { useCallback, useEffect, useRef, useState, type FormEvent, type MouseEvent, type ReactNode } from "react";
import { sileo } from "sileo";
import type { PaginaDTO } from "./models/factura.dto";
import type { ExplorerColumn } from "./PaginatedExplorerModal";
import { errorMessage } from "./formatters";
import { IconButton } from "./IconButton";
import { EntityContextMenu } from "./EntityContextMenu";

interface RowMenuState<T> {
  item: T;
  x: number;
  y: number;
}

interface PaginatedDataViewProps<T> {
  title: string;
  description: string;
  searchPlaceholder: string;
  emptyText: string;
  columns: ExplorerColumn<T>[];
  getKey: (item: T) => string | number;
  loadPage: (search: string, page: number, pageSize: number) => Promise<PaginaDTO<T>>;
  actionLabel: string;
  actionIcon: string;
  onAction: () => void;
  controls?: ReactNode;
  refreshKey?: number;
  tableClassName?: string;
  pageSize?: number;
  getItemLabel?: (item: T) => string;
  onEdit?: (item: T) => void;
  onDelete?: (item: T) => void;
}

export function PaginatedDataView<T,>({
  title, description, searchPlaceholder, emptyText, columns, getKey, loadPage,
  actionLabel, actionIcon, onAction, controls, refreshKey = 0, tableClassName = "", pageSize = 10,
  getItemLabel, onEdit, onDelete,
}: PaginatedDataViewProps<T>) {
  const [items, setItems] = useState<T[]>([]);
  const [searchDraft, setSearchDraft] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [rowMenu, setRowMenu] = useState<RowMenuState<T> | null>(null);
  const requestVersion = useRef(0);
  const closeRowMenu = useCallback(() => setRowMenu(null), []);
  const hasRowActions = Boolean(getItemLabel && onEdit && onDelete);

  const openAt = (item: T, x: number, y: number) => {
    if (!hasRowActions) return;
    setRowMenu({
      item,
      x: Math.max(8, Math.min(x, window.innerWidth - 198)),
      y: Math.max(8, Math.min(y, window.innerHeight - 116)),
    });
  };

  const openFromRow = (event: MouseEvent<HTMLTableRowElement>, item: T) => {
    event.preventDefault();
    openAt(item, event.clientX, event.clientY);
  };

  const openFromButton = (event: MouseEvent<HTMLButtonElement>, item: T) => {
    event.stopPropagation();
    const rect = event.currentTarget.getBoundingClientRect();
    openAt(item, rect.right - 180, rect.bottom + 4);
  };

  const requestPage = async (term: string, requestedPage: number) => {
    const version = ++requestVersion.current;
    setLoading(true);
    try {
      const result = await loadPage(term, requestedPage, pageSize);
      if (version !== requestVersion.current) return;
      setItems(result.contenido);
      setSearch(term);
      setPage(result.pagina);
      setTotalItems(result.totalElementos);
      setTotalPages(result.totalPaginas);
    } catch (error) {
      if (version !== requestVersion.current) return;
      setItems([]);
      sileo.error({ title: `No se pudieron cargar ${title.toLowerCase()}`, description: errorMessage(error) });
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  };

  useEffect(() => { void requestPage(search, 0); }, [loadPage, refreshKey]);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!loading) void requestPage(searchDraft.trim(), 0);
  };
  const first = totalItems === 0 ? 0 : page * pageSize + 1;
  const last = Math.min((page + 1) * pageSize, totalItems);

  return (
    <section className="data-view">
      <header className="page-heading">
        <div><h1>{title}</h1><p>{description}</p></div>
        <button type="button" data-variant="primary" onClick={onAction}><i className={`ph ${actionIcon}`} aria-hidden="true" /><span>{actionLabel}</span></button>
      </header>
      <form className="data-view-toolbar" onSubmit={submit}>
        <label className="explorer-search">
          <span className="search-input explorer-search-control">
            <input type="search" aria-label={`Buscar en ${title.toLowerCase()}`} value={searchDraft} placeholder={searchPlaceholder} onChange={(event) => setSearchDraft(event.target.value)} />
            <button type="submit" className="explorer-search-button" disabled={loading}><i className="ph ph-magnifying-glass" aria-hidden="true" /><span>Buscar</span></button>
          </span>
        </label>
        {controls && <div className="explorer-controls">{controls}</div>}
      </form>
      <div className="table-shell data-view-table-shell">
        <table className={tableClassName}>
          <thead><tr>{columns.map((column) => <th key={column.header} style={{ textAlign: column.align }}>{column.header}</th>)}{hasRowActions && <th className="row-menu-column"><span className="sr-only">Menú</span></th>}</tr></thead>
          <tbody>
            {loading && <tr><td colSpan={columns.length + (hasRowActions ? 1 : 0)} data-empty>Cargando…</td></tr>}
            {!loading && items.length === 0 && <tr><td colSpan={columns.length + (hasRowActions ? 1 : 0)} data-empty>{emptyText}</td></tr>}
            {!loading && items.map((item) => <tr key={getKey(item)} onContextMenu={hasRowActions ? (event) => openFromRow(event, item) : undefined}>
              {columns.map((column) => <td key={column.header} style={{ textAlign: column.align }}>{column.render(item)}</td>)}
              {hasRowActions && <td className="row-menu-column"><IconButton type="button" icon="ph-dots-three-vertical" label={`Acciones de ${getItemLabel?.(item) ?? "registro"}`} aria-haspopup="menu" aria-expanded={rowMenu?.item === item} onClick={(event) => openFromButton(event, item)} /></td>}
            </tr>)}
          </tbody>
        </table>
      </div>
      <footer className="data-view-pagination">
        <span>{first}–{last} de {totalItems}</span>
        <div>
          <button type="button" className="icon-button" aria-label="Página anterior" disabled={loading || page === 0} onClick={() => void requestPage(search, page - 1)}><i className="ph ph-caret-left" aria-hidden="true" /></button>
          <span>Página {page + 1} de {Math.max(1, totalPages)}</span>
          <button type="button" className="icon-button" aria-label="Página siguiente" disabled={loading || totalPages === 0 || page + 1 >= totalPages} onClick={() => void requestPage(search, page + 1)}><i className="ph ph-caret-right" aria-hidden="true" /></button>
        </div>
      </footer>
      {rowMenu && getItemLabel && onEdit && onDelete && <EntityContextMenu
        label={getItemLabel(rowMenu.item)}
        x={rowMenu.x}
        y={rowMenu.y}
        onEdit={() => onEdit(rowMenu.item)}
        onDelete={() => onDelete(rowMenu.item)}
        onClose={closeRowMenu}
      />}
    </section>
  );
}
