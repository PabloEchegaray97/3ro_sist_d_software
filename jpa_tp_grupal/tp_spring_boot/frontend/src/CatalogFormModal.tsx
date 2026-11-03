import { useEffect, useState, type FormEvent } from "react";
import { sileo } from "sileo";
import type { CatalogoItemDTO, MarcaRequestDTO, RubroRequestDTO } from "./models/factura.dto";
import { errorMessage } from "./formatters";
import { Modal } from "./Modal";
import { FacturaService } from "./services/FacturaService";

export type CatalogKind = "rubros" | "marcas";

interface CatalogFormModalProps {
  open: boolean;
  kind: CatalogKind;
  item: CatalogoItemDTO | null;
  service: FacturaService;
  onClose: () => void;
  onSaved: () => void;
}

const labels = {
  rubros: { singular: "rubro", title: "Rubro" },
  marcas: { singular: "marca", title: "Marca" },
} as const;

export function CatalogFormModal({ open, kind, item, service, onClose, onSaved }: CatalogFormModalProps) {
  const [denominacion, setDenominacion] = useState("");
  const [saving, setSaving] = useState(false);
  const label = labels[kind];

  useEffect(() => {
    if (!open) return;
    setDenominacion(item?.denominacion ?? "");
  }, [item, open]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    try {
      const options = await service.obtenerOpcionesFactura();
      if (!options.usuario) throw new Error("No hay un usuario disponible para registrar el cambio");
      const request: RubroRequestDTO | MarcaRequestDTO = {
        denominacion: denominacion.trim(),
        usuarioId: options.usuario.id,
      };
      if (kind === "rubros") {
        if (item) await service.actualizarRubro(item.id, request);
        else await service.crearRubro(request);
      } else if (item) {
        await service.actualizarMarca(item.id, request);
      } else {
        await service.crearMarca(request);
      }
      sileo.success({ title: item ? `${label.title} actualizado` : `${label.title} creado`, description: request.denominacion });
      onSaved();
    } catch (error) {
      sileo.error({
        title: item ? `No se pudo actualizar el ${label.singular}` : `No se pudo crear el ${label.singular}`,
        description: errorMessage(error),
      });
    } finally {
      setSaving(false);
    }
  };

  return <Modal open={open} title={item ? `Editar ${label.singular}` : `Nuevo ${label.singular}`} closeDisabled={saving} onClose={onClose}>
    <form onSubmit={submit}>
      <fieldset className="catalog-form-grid" disabled={saving}>
        <label><span>Denominación</span><input required value={denominacion} onChange={(event) => setDenominacion(event.target.value)} /></label>
      </fieldset>
      <footer><button type="submit" data-variant="primary" disabled={saving}><i className="ph ph-check" aria-hidden="true" /><span>{saving ? "Guardando…" : item ? "Guardar cambios" : `Crear ${label.singular}`}</span></button></footer>
    </form>
  </Modal>;
}
