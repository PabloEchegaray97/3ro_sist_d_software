// CONSIGNA ESCRITURA - 4.a, 5.a y 5.b: espejo exacto de los DTOs Java.
export interface FacturaReporteDTO {
  id: number;
  numeroFactura: number | null;
  fechaEmision: string | null;
  fechaCreacionInterna: string;
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
  numero: number | null;
  fechaEmision: string | null;
  puntoVentaId: number;
  puntoVentaNumero: number;
  puntoVentaDescripcion: string;
  puntoVentaDomicilio: string | null;
  tipoComprobante: number;
  clienteId: number | null;
  clienteTipoDocumento: "CUIT" | "CUIL" | null;
  clienteDenominacion: string;
  clienteCuitCuil: string | null;
  clienteTelefono: string | null;
  clienteEmail: string | null;
  clienteDomicilio: string | null;
  listaPrecioId: number | null;
  listaPrecioCodigo: string | null;
  listaPrecio: string | null;
  condicionIvaId: number;
  condicionIva: string;
  tipoMonedaId: number;
  tipoMonedaCodigo: string;
  tipoMoneda: string;
  usuarioId: number;
  usuario: string;
  estado: string;
  importeCobrado: number;
  importeSaldo: number;
  importeTotal: number;
  observaciones: string | null;
  fechaAnulacion: string | null;
  cae: string | null;
  caeFechaVencimiento: string | null;
  resultadoAfip: string | null;
  motivoRechazo: string | null;
  detalles: FacturaDetalleVistaDTO[];
}

export interface MensajeDTO {
  tipo: string;
  codigo: string;
  descripcion: string;
}

export interface ArcaEmisionDTO {
  emitida: boolean;
  facturaId: number;
  numero: number | null;
  estado: string;
  cae: string | null;
  caeFechaVencimiento: string | null;
  mensaje: string;
  mensajes: MensajeDTO[];
}

export interface ArcaAutenticacionDTO {
  autenticada: boolean;
  ambiente: string;
  servicio: string;
  vencimiento: string | null;
  mensaje: string;
}

export interface ArcaConfiguracionDTO {
  ambiente: string;
  cuit: string | null;
  condicionIvaEmisor: string;
  razonSocialEmisor: string;
  ingresosBrutosEmisor: string;
  inicioActividadesEmisor: string;
  telefonoEmisor: string;
  logoPath: string;
  tipoComprobanteCodigo: number;
  tipoComprobanteDescripcion: string;
  cuitConfigurado: boolean;
  keystorePath: string | null;
  keystoreConfigurado: boolean;
  keystoreExiste: boolean;
  passwordConfigurada: boolean;
  wsaaUrl: string;
  wsfeUrl: string;
  endpointsHomologacion: boolean;
  listaParaProbar: boolean;
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
  precioVenta: number;
  fechaBaja: string | null;
}

export interface ClienteDisponibleDTO {
  id: number;
  tipoDocumento: "CUIT" | "CUIL";
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
  fechaBaja: string | null;
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
  puntosVenta: OpcionDTO[];
  clientes: ClienteOpcionDTO[];
  condicionesIva: OpcionDTO[];
  tiposMoneda: OpcionDTO[];
  listasPrecio: OpcionDTO[];
  listaPrecioConsumidorFinal: OpcionDTO | null;
  usuario: OpcionDTO | null;
}

export interface FacturaRequestDTO {
  puntoVentaId: number;
  clienteId: number | null;
  listaPrecioId: number;
  condicionIvaId: number;
  tipoMonedaId: number;
  usuarioId: number;
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
export interface UsuarioRequestDTO {
  usuario: string;
  clave: string;
  nombre: string;
  apellido: string;
}

export interface CondicionIvaRequestDTO {
  codigoAfip: number;
  denominacion: string;
  usuarioId: number;
}

export interface TipoMonedaRequestDTO {
  codigoAfip: string;
  denominacion: string;
  simbolo: string;
  usuarioId: number;
}

export interface PuntoVentaRequestDTO {
  numero: number;
  descripcion: string;
  tipoEmision: string;
  domicilioComercial: string;
  usuarioId: number;
}

export interface ClienteRequestDTO {
  tipoDocumento: "CUIT" | "CUIL";
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

export interface ArticuloRequestDTO {
  codigo: string;
  denominacion: string;
  rubroId: number;
  marcaId: number | null;
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
  codigo: string | null;
  denominacion: string;
  rubroId: number;
  marcaId: number | null;
  usuarioId: number;
  precios: ArticuloAltaPrecioRequestDTO[];
}

export interface ArticuloEdicionDTO {
  id: number;
  codigo: string;
  denominacion: string;
  rubroId: number;
  marcaId: number | null;
  precios: ArticuloAltaPrecioRequestDTO[];
}

export interface EntidadCreadaDTO {
  id: number;
  descripcion: string;
}

export interface CatalogoItemDTO {
  id: number;
  codigo: number;
  denominacion: string;
  fechaAlta: string;
  fechaBaja: string | null;
}

export interface RubroRequestDTO {
  denominacion: string;
  usuarioId: number;
}

export interface MarcaRequestDTO {
  denominacion: string;
  usuarioId: number;
}

export interface SistemaInfoDTO {
  puntosVenta: PuntoVentaInfoDTO[];
  condicionesIva: CondicionIvaInfoDTO[];
  monedas: MonedaInfoDTO[];
  listasPrecio: ListaPrecioInfoDTO[];
  usuario: UsuarioInfoDTO | null;
}

export interface PuntoVentaInfoDTO {
  id: number;
  numero: number;
  descripcion: string;
  tipoEmision: string | null;
  domicilioComercial: string | null;
}

export interface CondicionIvaInfoDTO {
  id: number;
  codigoAfip: number;
  denominacion: string;
}

export interface MonedaInfoDTO {
  id: number;
  codigoAfip: string;
  denominacion: string;
  simbolo: string;
}

export interface ListaPrecioInfoDTO {
  id: number;
  codigo: string;
  denominacion: string;
}

export interface UsuarioInfoDTO {
  id: number;
  usuario: string;
  nombreCompleto: string;
}

export interface TotalMonedaDTO {
  codigoMoneda: string;
  moneda: string;
  total: number;
}
