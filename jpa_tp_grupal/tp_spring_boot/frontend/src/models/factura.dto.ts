// CONSIGNA ESCRITURA - 4.a, 5.a y 5.b: espejo exacto de los DTOs Java.
export interface FacturaReporteDTO {
  id: number;
  numeroFactura: number;
  fechaEmision: string;
  clienteDenominacion: string;
  condicionIva: string;
  puntoVentaDescripcion: string;
  importeTotal: number;
  cantidadItems: number;
  estado: string;
}

export interface FacturaDetalleRequestDTO {
  articuloId: number;
  listaPrecioArticuloId: number;
  descripcion: string | null;
  cantidad: number;
  precioUnitario: number | null;
  porcentajeBonificacion: number | null;
  porcentajeIva: number | null;
}

export interface FacturaDetalleVistaDTO {
  id: number;
  articuloId: number;
  articuloCodigo: string;
  articuloDenominacion: string;
  listaPrecioArticuloId: number;
  listaPrecio: string;
  descripcion: string | null;
  cantidad: number;
  precioUnitario: number;
  porcentajeBonificacion: number;
  porcentajeIva: number;
  importeNeto: number;
  importeIva: number;
  importeSubtotal: number;
}

export interface FacturaVistaDTO {
  id: number;
  numero: number;
  fechaEmision: string;
  puntoVentaId: number;
  puntoVentaDescripcion: string;
  clienteId: number | null;
  clienteDenominacion: string;
  condicionIvaId: number;
  condicionIva: string;
  tipoMonedaId: number;
  tipoMoneda: string;
  usuarioId: number;
  usuario: string;
  estado: string;
  importeCobrado: number;
  importeSaldo: number;
  importeTotal: number;
  observaciones: string | null;
  fechaAnulacion: string | null;
  detalles: FacturaDetalleVistaDTO[];
}

export interface ArticuloDisponibleDTO {
  listaPrecioArticuloId: number;
  articuloId: number;
  codigo: string;
  denominacion: string;
  listaPrecio: string;
  precioVenta: number;
}

export interface OpcionDTO {
  id: number;
  descripcion: string;
}

export interface ClienteOpcionDTO extends OpcionDTO {
  condicionIvaId: number | null;
}

export interface FacturaOpcionesDTO {
  siguienteNumero: number;
  puntosVenta: OpcionDTO[];
  clientes: ClienteOpcionDTO[];
  condicionesIva: OpcionDTO[];
  tiposMoneda: OpcionDTO[];
  usuario: OpcionDTO | null;
}

export interface FacturaRequestDTO {
  numero: number;
  fechaEmision: string;
  puntoVentaId: number;
  clienteId: number | null;
  condicionIvaId: number;
  tipoMonedaId: number;
  usuarioId: number;
  estado: string;
  importeCobrado: number | null;
  observaciones: string | null;
  detalles: FacturaDetalleRequestDTO[];
}

export interface FacturaFiltrosDTO {
  fechaDesde?: string;
  fechaHasta?: string;
  estado?: string;
  montoMinimo?: number;
}
