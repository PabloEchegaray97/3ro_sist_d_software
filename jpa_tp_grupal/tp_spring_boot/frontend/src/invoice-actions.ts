import type { FacturaReporteDTO } from "./models/factura.dto";

export type InvoiceAction = "view" | "edit" | "annul" | "delete";

export interface PendingInvoiceAction {
  action: "annul" | "delete";
  invoice: FacturaReporteDTO;
}
