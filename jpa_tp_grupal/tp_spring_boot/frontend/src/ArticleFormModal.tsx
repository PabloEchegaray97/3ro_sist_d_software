import { useEffect, useRef, useState, type FormEvent } from "react";
import { sileo } from "sileo";
import type { ArticuloAltaOpcionesDTO, ArticuloAltaRequestDTO } from "./models/factura.dto";
import { FacturaService } from "./services/FacturaService";
import { errorMessage } from "./formatters";
import { Modal } from "./Modal";
import { fullPriceListLabel, priceListTier } from "./price-list-labels";

interface ArticleFormModalProps {
  open: boolean;
  articleId: number | null;
  service: FacturaService;
  onClose: () => void;
  onSaved: () => void;
}

export function ArticleFormModal({ open, articleId, service, onClose, onSaved }: ArticleFormModalProps) {
  const [options, setOptions] = useState<ArticuloAltaOpcionesDTO | null>(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ denominacion: "", rubroId: "", marcaId: "" });
  const [selectedLists, setSelectedLists] = useState<Record<number, boolean>>({});
  const [prices, setPrices] = useState<Record<number, string>>({});
  const priceInputs = useRef<Record<number, HTMLInputElement | null>>({});

  useEffect(() => {
    if (!open) return;
    let active = true;
    setForm({ denominacion: "", rubroId: "", marcaId: "" });
    setSelectedLists({}); setPrices({});
    Promise.all([
      service.obtenerOpcionesAltaArticulo(),
      articleId == null ? Promise.resolve(null) : service.obtenerArticuloEdicion(articleId),
    ]).then(([result, article]) => {
      if (!active) return;
      setOptions(result);
      if (article) {
        setForm({
          denominacion: article.denominacion,
          rubroId: String(article.rubroId),
          marcaId: article.marcaId == null ? "" : String(article.marcaId),
        });
        setSelectedLists(Object.fromEntries(article.precios.map((price) => [price.listaPrecioId, true])));
        setPrices(Object.fromEntries(article.precios.map((price) => [price.listaPrecioId, String(price.precioVenta)])));
      } else {
        setForm((current) => ({ ...current, rubroId: String(result.rubros[0]?.id ?? "") }));
      }
    }).catch((error) => { if (active) { sileo.error({ title: "No se pudo preparar el artículo", description: errorMessage(error) }); onClose(); } });
    return () => { active = false; };
  }, [articleId, open, onClose, service]);

  const changeSelectedList = (listId: number, selected: boolean) => {
    setSelectedLists((current) => ({ ...current, [listId]: selected }));
    if (selected) {
      requestAnimationFrame(() => {
        const input = priceInputs.current[listId];
        input?.focus();
        input?.select();
      });
    }
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (saving) return;
    if (!options?.usuario) return;
    const selectedPrices = options.listasPrecio.filter((list) => selectedLists[list.id]).map((list) => ({ listaPrecioId: list.id, precioVenta: Number(prices[list.id] ?? 0) }));
    if (selectedPrices.length === 0) { sileo.error({ title: "Seleccioná al menos una lista de precios" }); return; }
    const request: ArticuloAltaRequestDTO = {
      codigo: null, denominacion: form.denominacion, rubroId: Number(form.rubroId),
      marcaId: form.marcaId ? Number(form.marcaId) : null, usuarioId: options.usuario.id, precios: selectedPrices,
    };
    setSaving(true);
    try {
      if (articleId == null) await service.crearArticuloConPrecios(request);
      else await service.actualizarArticulo(articleId, request);
      sileo.success({ title: articleId == null ? "Artículo creado" : "Artículo actualizado", description: form.denominacion });
      onSaved();
    }
    catch (error) { sileo.error({ title: articleId == null ? "No se pudo crear el artículo" : "No se pudo actualizar el artículo", description: errorMessage(error) }); }
    finally { setSaving(false); }
  };

  return <Modal open={open} title={articleId == null ? "Nuevo artículo" : "Editar artículo"} className="wide-dialog" closeDisabled={saving} onClose={onClose}>
    <form onSubmit={submit}>
      <fieldset className="entity-form-grid article-main-fields" disabled={saving}>
        <label><span>Denominación</span><input required value={form.denominacion} onChange={(e) => setForm({ ...form, denominacion: e.target.value })} /></label>
        <label><span>Rubro</span><select required value={form.rubroId} onChange={(e) => setForm({ ...form, rubroId: e.target.value })}>{options?.rubros.map((item) => <option key={item.id} value={item.id}>{item.descripcion}</option>)}</select></label>
        <label><span>Marca</span><select value={form.marcaId} onChange={(e) => setForm({ ...form, marcaId: e.target.value })}><option value="">Sin marca</option>{options?.marcas.map((item) => <option key={item.id} value={item.id}>{item.descripcion}</option>)}</select></label>
      </fieldset>
      <fieldset className="price-list-fields" disabled={saving}>
        <legend>Listas de precios</legend>
        {options?.listasPrecio.map((list) => <div className="price-list-row" key={list.id}>
          <label className="price-list-check"><input type="checkbox" role="switch" checked={Boolean(selectedLists[list.id])} onChange={(e) => changeSelectedList(list.id, e.target.checked)} /><span className="price-list-name" data-price-tier={priceListTier(list.codigo, list.denominacion)}>{fullPriceListLabel(list.codigo, list.denominacion)}</span></label>
          <label><span>Precio final</span><input ref={(node) => { priceInputs.current[list.id] = node; }} type="number" min="0" step="0.01" disabled={!selectedLists[list.id]} required={selectedLists[list.id]} value={prices[list.id] ?? ""} onChange={(e) => setPrices((current) => ({ ...current, [list.id]: e.target.value }))} /></label>
        </div>)}
      </fieldset>
      <footer><button type="submit" data-variant="primary" disabled={saving || !options}><i className="ph ph-check" aria-hidden="true" /><span>{saving ? "Guardando…" : articleId == null ? "Crear artículo" : "Guardar cambios"}</span></button></footer>
    </form>
  </Modal>;
}
