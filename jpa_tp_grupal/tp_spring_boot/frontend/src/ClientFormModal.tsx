import { useEffect, useState, type FormEvent } from "react";
import { sileo } from "sileo";
import type { ClienteDisponibleDTO, ClienteRequestDTO, FacturaOpcionesDTO } from "./models/factura.dto";
import { FacturaService } from "./services/FacturaService";
import { errorMessage } from "./formatters";
import { Modal } from "./Modal";

interface ClientFormModalProps {
  open: boolean;
  client: ClienteDisponibleDTO | null;
  service: FacturaService;
  onClose: () => void;
  onSaved: () => void;
}

export function ClientFormModal({ open, client, service, onClose, onSaved }: ClientFormModalProps) {
  const [options, setOptions] = useState<FacturaOpcionesDTO | null>(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ cuitCuil: "", denominacion: "", condicionIvaId: "", listaPrecioId: "", email: "", telefono: "", celular: "", nombreCalle: "", numeroCalle: "" });

  useEffect(() => {
    if (!open) return;
    let active = true;
    setForm({
      cuitCuil: client?.cuitCuil ?? "",
      denominacion: client?.denominacion ?? "",
      condicionIvaId: client?.condicionIvaId == null ? "" : String(client.condicionIvaId),
      listaPrecioId: client?.listaPrecioId == null ? "" : String(client.listaPrecioId),
      email: client?.email ?? "",
      telefono: client?.telefono ?? "",
      celular: client?.celular ?? "",
      nombreCalle: client?.nombreCalle ?? "",
      numeroCalle: client?.numeroCalle ?? "",
    });
    service.obtenerOpcionesFactura().then((result) => {
      if (!active) return;
      setOptions(result);
      if (!client) {
        const consumidorFinal = result.condicionesIva.find((item) =>
          item.descripcion.trim().toLocaleLowerCase("es-AR").includes("consumidor final")
        );
        setForm((current) => ({
          ...current,
          condicionIvaId: String(consumidorFinal?.id ?? result.condicionesIva[0]?.id ?? ""),
          listaPrecioId: String(result.listaPrecioConsumidorFinal?.id ?? result.listasPrecio[0]?.id ?? ""),
        }));
      }
    }).catch((error) => { if (active) { sileo.error({ title: "No se pudo preparar el cliente", description: errorMessage(error) }); onClose(); } });
    return () => { active = false; };
  }, [client, open, onClose, service]);

  const update = (field: keyof typeof form, value: string) => setForm((current) => ({ ...current, [field]: value }));
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (saving) return;
    if (!options?.usuario) return;
    const request: ClienteRequestDTO = {
      cuitCuil: form.cuitCuil, denominacion: form.denominacion,
      email: form.email || null, telefono: form.telefono || null, celular: form.celular || null,
      nombreCalle: form.nombreCalle || null, numeroCalle: form.numeroCalle || null,
      condicionIvaId: Number(form.condicionIvaId), listaPrecioId: form.listaPrecioId ? Number(form.listaPrecioId) : null,
      usuarioId: options.usuario.id,
    };
    setSaving(true);
    try {
      if (client) await service.actualizarCliente(client.id, request);
      else await service.crearCliente(request);
      sileo.success({ title: client ? "Cliente actualizado" : "Cliente creado", description: form.denominacion });
      onSaved();
    }
    catch (error) { sileo.error({ title: client ? "No se pudo actualizar el cliente" : "No se pudo crear el cliente", description: errorMessage(error) }); }
    finally { setSaving(false); }
  };

  return <Modal open={open} title={client ? "Editar cliente" : "Nuevo cliente"} className="wide-dialog" closeDisabled={saving} onClose={onClose}>
    <form onSubmit={submit}>
      <fieldset className="entity-form-grid" disabled={saving}>
        <label><span>CUIT/CUIL</span><input required value={form.cuitCuil} onChange={(e) => update("cuitCuil", e.target.value)} /></label>
        <label><span>Denominación</span><input required value={form.denominacion} onChange={(e) => update("denominacion", e.target.value)} /></label>
        <label><span>Condición de IVA</span><select required value={form.condicionIvaId} onChange={(e) => update("condicionIvaId", e.target.value)}>{options?.condicionesIva.map((item) => <option key={item.id} value={item.id}>{item.descripcion}</option>)}</select></label>
        <label><span>Lista de precios</span><select value={form.listaPrecioId} onChange={(e) => update("listaPrecioId", e.target.value)}>{options?.listasPrecio.map((item) => <option key={item.id} value={item.id}>{item.descripcion}</option>)}</select></label>
        <label><span>Teléfono</span><input value={form.telefono} onChange={(e) => update("telefono", e.target.value)} /></label>
        <label><span>Celular</span><input value={form.celular} onChange={(e) => update("celular", e.target.value)} /></label>
        <label><span>Email</span><input type="email" value={form.email} onChange={(e) => update("email", e.target.value)} /></label>
        <label><span>Calle</span><input value={form.nombreCalle} onChange={(e) => update("nombreCalle", e.target.value)} /></label>
        <label><span>Número</span><input value={form.numeroCalle} onChange={(e) => update("numeroCalle", e.target.value)} /></label>
      </fieldset>
      <footer><button type="submit" data-variant="primary" disabled={saving || !options}><i className="ph ph-check" aria-hidden="true" /><span>{saving ? "Guardando…" : client ? "Guardar cambios" : "Crear cliente"}</span></button></footer>
    </form>
  </Modal>;
}
