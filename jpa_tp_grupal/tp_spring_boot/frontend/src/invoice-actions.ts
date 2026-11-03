import type { FacturaReporteDTO } from "./models/factura.dto";

export type InvoiceAction = "view" | "edit" | "emit" | "annul" | "delete";

export interface PendingInvoiceAction {
  action: "annul" | "delete";
  invoice: FacturaReporteDTO;
}
