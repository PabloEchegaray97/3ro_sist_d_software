import type { ArticuloDisponibleDTO, FacturaDetalleRequestDTO } from "./models/factura.dto";
import { roundMoney } from "./formatters";

export function detailFromArticle(article: ArticuloDisponibleDTO): FacturaDetalleRequestDTO {
  return {
    articuloId: article.articuloId,
    listaPrecioArticuloId: article.listaPrecioArticuloId,
    descripcion: article.denominacion,
    cantidad: 1,
    precioUnitario: article.precioVenta,
    porcentajeBonificacion: 0,
    porcentajeIva: 21,
  };
}

export interface InvoiceDetailAmounts {
  unitPrice: number;
  gross: number;
  discountAmount: number;
  net: number;
  ivaPercent: number;
  ivaAmount: number;
  subtotal: number;
}

export function calculateDetailAmounts(
  detail: FacturaDetalleRequestDTO,
  articles: ArticuloDisponibleDTO[] = [],
): InvoiceDetailAmounts {
  const listPrice = articles.find(
    (article) => article.listaPrecioArticuloId === detail.listaPrecioArticuloId,
  )?.precioVenta ?? 0;
  const unitPrice = detail.precioUnitario ?? listPrice;
  const discount = detail.porcentajeBonificacion ?? 0;
  const ivaPercent = detail.porcentajeIva ?? 21;
  const gross = roundMoney(detail.cantidad * unitPrice);
  const discountAmount = roundMoney(gross * discount / 100);
  const net = roundMoney(gross - discountAmount);
  const ivaAmount = roundMoney(net * ivaPercent / 100);
  return {
    unitPrice,
    gross,
    discountAmount,
    net,
    ivaPercent,
    ivaAmount,
    subtotal: roundMoney(net + ivaAmount),
  };
}

export function calculateTotal(
  details: FacturaDetalleRequestDTO[],
  articles: ArticuloDisponibleDTO[],
): number {
  return roundMoney(details.reduce(
    (total, detail) => total + calculateDetailAmounts(detail, articles).subtotal,
    0,
  ));
}
