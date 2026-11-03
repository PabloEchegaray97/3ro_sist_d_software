import { useId } from "react";
import type {
  ArticuloDisponibleDTO,
  FacturaDetalleRequestDTO,
  FacturaDetalleVistaDTO,
} from "./models/factura.dto";
import { money } from "./formatters";
import { calculateDetailAmounts } from "./invoice-utils";
import { IconButton } from "./IconButton";

interface InvoiceDetailEditorRowProps {
  detail: FacturaDetalleRequestDTO;
  article?: ArticuloDisponibleDTO;
  persistedDetail?: FacturaDetalleVistaDTO;
  onChange: (field: "cantidad" | "porcentajeBonificacion", value: string) => void;
  onRemove: () => void;
}

export function InvoiceDetailEditorRow({
  detail,
  article,
  persistedDetail,
  onChange,
  onRemove,
}: InvoiceDetailEditorRowProps) {
  const calculationTooltipId = useId();
  const amounts = calculateDetailAmounts(detail, article ? [article] : []);
  const code = article?.codigo ?? persistedDetail?.articuloCodigo ?? `Artículo ${detail.articuloId}`;
  const description = article?.denominacion
    ?? persistedDetail?.articuloDenominacion
    ?? detail.descripcion
    ?? "Sin descripción";

  return (
    <tr>
      <td className="detail-product-cell">
        <strong>{code}</strong>
        <span>{description}</span>
        <small>Artículo {detail.articuloId} · Precio lista {detail.listaPrecioArticuloId}</small>
      </td>
      <td className="detail-money-cell"><strong>{money(amounts.unitPrice)}</strong><small>por unidad</small></td>
      <td className="detail-control-cell">
        <div className="detail-input-control">
          <input
            aria-label={`Cantidad de ${description}`}
            type="number"
            min="1"
            step="1"
            required
            value={detail.cantidad}
            onKeyDown={(event) => {
              if (["-", "+", "e", "E", ".", ","].includes(event.key)) event.preventDefault();
            }}
            onChange={(event) => {
              if (/^\d*$/.test(event.target.value)) onChange("cantidad", event.target.value);
            }}
          />
        </div>
        <small>unidades</small>
      </td>
      <td className="detail-control-cell detail-discount-cell">
        <div className="detail-percent-control">
          <input
            aria-label={`Bonificación de ${description}`}
            type="number"
            min="0"
            max="100"
            step="1"
            value={detail.porcentajeBonificacion ?? 0}
            onKeyDown={(event) => {
              if (["-", "+", "e", "E", ".", ","].includes(event.key)) event.preventDefault();
            }}
            onChange={(event) => {
              if (/^\d*$/.test(event.target.value)) onChange("porcentajeBonificacion", event.target.value);
            }}
          />
          <span>%</span>
        </div>
        <small>- {money(amounts.discountAmount)}</small>
      </td>
      <td className="detail-money-cell detail-net-cell">
        <div className="detail-net-value">
          <strong>{money(amounts.net)}</strong>
          <button type="button" className="detail-info-button" aria-label={`Ver cálculo del neto de ${description}`} aria-describedby={calculationTooltipId}>
            <i className="ph ph-info" aria-hidden="true" />
            <span id={calculationTooltipId} className="detail-calculation-tooltip" role="tooltip">
              <span>{money(amounts.unitPrice)} × {detail.cantidad} = {money(amounts.gross)}</span>
              <span>{money(amounts.gross)} − {amounts.discountAmount > 0 ? `${detail.porcentajeBonificacion ?? 0}% (${money(amounts.discountAmount)})` : money(0)} = {money(amounts.net)}</span>
            </span>
          </button>
        </div>
        <small>después de bonificación</small>
      </td>
      <td className="detail-money-cell"><strong>{money(amounts.ivaAmount)}</strong><small>{amounts.ivaPercent}%</small></td>
      <td className="detail-money-cell detail-subtotal-cell"><strong>{money(amounts.subtotal)}</strong><small>total del renglón</small></td>
      <td className="detail-remove-cell"><IconButton type="button" className="detail-delete-button" icon="ph-trash" label={`Quitar ${description}`} onClick={onRemove} /></td>
    </tr>
  );
}
