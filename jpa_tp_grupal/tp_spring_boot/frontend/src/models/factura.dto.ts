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
  listaPrecioId: number | null;
  listaPrecioCodigo: string | null;
  listaPrecio: string | null;
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
  rubroCodigo: number;
  rubro: string;
  marcaCodigo: number | null;
  marca: string | null;
  listaPrecioId: number;
  listaPrecioCodigo: string;
  listaPrecio: string;
  precioVenta: number;
}

export interface ClienteDisponibleDTO {
  id: number;
  cuitCuil: string;
  denominacion: string;
  condicionIvaId: number | null;
  condicionIva: string | null;
  email: string | null;
  telefono: string | null;
  celular: string | null;
  nombreCalle: string | null;
  numeroCalle: string | null;
  listaPrecioId: number | null;
  listaPrecioCodigo: string | null;
  listaPrecio: string | null;
  fechaAlta: string;
  ultimaCompra: string | null;
}

export interface PaginaDTO<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface OpcionDTO {
  id: number;
  descripcion: string;
}

export interface ClienteOpcionDTO extends OpcionDTO {
  condicionIvaId: number | null;
  listaPrecioId: number | null;
  listaPrecio: string | null;
}

export interface FacturaOpcionesDTO {
  siguienteNumero: number;
  puntosVenta: OpcionDTO[];
  clientes: ClienteOpcionDTO[];
  condicionesIva: OpcionDTO[];
  tiposMoneda: OpcionDTO[];
  listasPrecio: OpcionDTO[];
  listaPrecioConsumidorFinal: OpcionDTO | null;
  usuario: OpcionDTO | null;
}

export interface FacturaRequestDTO {
  numero: number;
  fechaEmision: string;
  puntoVentaId: number;
  clienteId: number | null;
  listaPrecioId: number;
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

// Espejo de DatosMaestrosDTO.java para mantener iguales los contratos de escritura.
export interface ClienteRequestDTO {
  cuitCuil: string;
  denominacion: string;
  email: string | null;
  telefono: string | null;
  celular: string | null;
  nombreCalle: string | null;
  numeroCalle: string | null;
  condicionIvaId: number;
  listaPrecioId: number | null;
  usuarioId: number;
}

export interface ClienteListaPrecioRequestDTO {
  listaPrecioId: number;
  usuarioId: number;
}

export interface ListaPrecioRequestDTO {
  codigo: string;
  denominacion: string;
  usuarioId: number;
}

export interface ListaPrecioArticuloRequestDTO {
  listaPrecioId: number;
  articuloId: number;
  precioVenta: number;
  usuarioId: number;
}

export interface ArticuloAltaOpcionDTO {
  id: number;
  descripcion: string;
}

export interface ArticuloAltaListaPrecioOpcionDTO {
  id: number;
  codigo: string;
  denominacion: string;
}

export interface ArticuloAltaOpcionesDTO {
  rubros: ArticuloAltaOpcionDTO[];
  marcas: ArticuloAltaOpcionDTO[];
  listasPrecio: ArticuloAltaListaPrecioOpcionDTO[];
  usuario: ArticuloAltaOpcionDTO | null;
}

export interface ArticuloAltaPrecioRequestDTO {
  listaPrecioId: number;
  precioVenta: number;
}

export interface ArticuloAltaRequestDTO {
  codigo: string;
  denominacion: string;
  rubroId: number;
  marcaId: number | null;
  usuarioId: number;
  precios: ArticuloAltaPrecioRequestDTO[];
}

export interface EntidadCreadaDTO {
  id: number;
  descripcion: string;
}
