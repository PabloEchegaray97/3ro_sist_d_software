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

export function calculateTotal(
  details: FacturaDetalleRequestDTO[],
  articles: ArticuloDisponibleDTO[],
): number {
  return roundMoney(details.reduce((total, detail) => {
    const listPrice = articles.find(
      (article) => article.listaPrecioArticuloId === detail.listaPrecioArticuloId,
    )?.precioVenta ?? 0;
    const unitPrice = detail.precioUnitario ?? listPrice;
    const discount = detail.porcentajeBonificacion ?? 0;
    const iva = detail.porcentajeIva ?? 21;
    const gross = detail.cantidad * unitPrice;
    const net = roundMoney(gross - gross * discount / 100);
    return total + roundMoney(net + net * iva / 100);
  }, 0));
}
