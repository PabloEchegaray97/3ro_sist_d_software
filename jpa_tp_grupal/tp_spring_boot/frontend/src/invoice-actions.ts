import type { FacturaReporteDTO } from "./models/factura.dto";

export type InvoiceAction = "view" | "edit" | "emit" | "delete";

export interface PendingInvoiceAction {
  action: "delete";
  invoice: FacturaReporteDTO;
}
