import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { sileo } from "sileo";
import type { PaginaDTO } from "./models/factura.dto";
import { errorMessage } from "./formatters";
import { Modal } from "./Modal";

export interface ExplorerColumn<T> {
  header: string;
  render: (item: T) => ReactNode;
  searchValue: (item: T) => unknown;
  align?: "left" | "right" | "center";
}

interface PaginatedExplorerModalProps<T> {
  open: boolean;
  title: string;
  description?: string;
  searchPlaceholder: string;
  columns: ExplorerColumn<T>[];
  getKey: (item: T) => string | number;
  emptyText: string;
  selectLabel?: string;
  selectIcon?: string;
  iconOnlyAction?: boolean;
  selectOnRowClick?: boolean;
  controls?: ReactNode;
  requestDependency?: unknown;
  tableClassName?: string;
  tableShellClassName?: string;
  pageSize?: number;
  loadPage: (search: string, page: number, pageSize: number) => Promise<PaginaDTO<T>>;
  onSelect: (item: T) => void;
  onClose: () => void;
}

export function PaginatedExplorerModal<T,>({
  open,
  title,
  description,
  searchPlaceholder,
  columns,
  getKey,
  emptyText,
  selectLabel = "Seleccionar",
  selectIcon = "ph-check",
  iconOnlyAction = false,
  selectOnRowClick = false,
  controls,
  requestDependency,
  tableClassName = "",
  tableShellClassName = "",
  pageSize = 8,
  loadPage,
  onSelect,
  onClose,
}: PaginatedExplorerModalProps<T>) {
  const [items, setItems] = useState<T[]>([]);
  const [draftSearch, setDraftSearch] = useState("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [page, setPage] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  const requestPage = async (search: string, requestedPage: number) => {
    setLoading(true);
    try {
      const result = await loadPage(search, requestedPage, pageSize);
      setItems(result.contenido);
      setAppliedSearch(search);
      setPage(result.pagina);
      setTotalItems(result.totalElementos);
      setTotalPages(result.totalPaginas);
    } catch (error) {
      setItems([]);
      setTotalItems(0);
      setTotalPages(0);
      sileo.error({ title: `No se pudo realizar la búsqueda`, description: errorMessage(error) });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!open) return;
    setDraftSearch("");
    setAppliedSearch("");
    void requestPage("", 0);
  }, [open]);

  useEffect(() => {
    if (!open) return;
    void requestPage(appliedSearch, 0);
  }, [requestDependency]);

  const submitSearch = (event: FormEvent) => {
    event.preventDefault();
    if (!loading) void requestPage(draftSearch.trim(), 0);
  };

  const first = totalItems === 0 ? 0 : page * pageSize + 1;
  const last = Math.min((page + 1) * pageSize, totalItems);
  const displayedTotalPages = Math.max(1, totalPages);

  return (
    <Modal open={open} title={title} description={description} className="explorer-dialog" onClose={onClose}>
      <form className="explorer-toolbar" onSubmit={submitSearch}>
        <label className="explorer-search">
          <span>Buscar</span>
          <span className="search-input explorer-search-control">
            <input
              type="search"
              autoFocus={open}
              value={draftSearch}
              placeholder={searchPlaceholder}
              onChange={(event) => setDraftSearch(event.target.value)}
            />
            <button type="submit" className="explorer-search-button" disabled={loading}>
              <i className="ph ph-magnifying-glass" aria-hidden="true" />
              <span>{loading ? "Buscando…" : "Buscar"}</span>
            </button>
          </span>
        </label>
        {controls && <div className="explorer-controls">{controls}</div>}
      </form>

      <div className={`table-shell explorer-table-shell ${tableShellClassName}`.trim()}>
        <table className={`explorer-table ${tableClassName}`.trim()}>
          <thead><tr>{columns.map((column) => <th key={column.header} style={{ textAlign: column.align }}>{column.header}</th>)}<th><span className="sr-only">Seleccionar</span></th></tr></thead>
          <tbody>
            {loading && <tr><td colSpan={columns.length + 1} data-empty>Buscando…</td></tr>}
            {!loading && items.length === 0 && <tr><td colSpan={columns.length + 1} data-empty>{emptyText}</td></tr>}
            {!loading && items.map((item) => (
              <tr
                key={getKey(item)}
                className={selectOnRowClick ? "selectable-row" : undefined}
                onClick={selectOnRowClick ? () => onSelect(item) : undefined}
                onDoubleClick={selectOnRowClick ? undefined : () => onSelect(item)}
              >
                {columns.map((column) => <td key={column.header} style={{ textAlign: column.align }}>{column.render(item)}</td>)}
                <td className={`explorer-select-column${iconOnlyAction ? " icon-only-column" : ""}`}>
                  <button
                    type="button"
                    className={iconOnlyAction ? "icon-button" : undefined}
                    data-variant={iconOnlyAction ? undefined : "primary"}
                    aria-label={iconOnlyAction ? selectLabel : undefined}
                    title={iconOnlyAction ? selectLabel : undefined}
                    onClick={(event) => {
                      event.stopPropagation();
                      onSelect(item);
                    }}
                  >
                    <i className={`ph ${selectIcon}`} aria-hidden="true" />
                    {!iconOnlyAction && <span>{selectLabel}</span>}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <footer className="explorer-pagination">
        <span>{first}–{last} de {totalItems}</span>
        <div>
          <button type="button" className="icon-button" aria-label="Página anterior" title="Página anterior" disabled={loading || page === 0} onClick={() => void requestPage(appliedSearch, page - 1)}><i className="ph ph-caret-left" aria-hidden="true" /></button>
          <span>Página {page + 1} de {displayedTotalPages}</span>
          <button type="button" className="icon-button" aria-label="Página siguiente" title="Página siguiente" disabled={loading || totalPages === 0 || page + 1 >= totalPages} onClick={() => void requestPage(appliedSearch, page + 1)}><i className="ph ph-caret-right" aria-hidden="true" /></button>
        </div>
      </footer>
    </Modal>
  );
}
