import type {
  ArticuloAltaOpcionesDTO,
  ArticuloAltaRequestDTO,
  ArticuloAltaOpcionDTO,
  ArticuloAltaListaPrecioOpcionDTO,
  ArticuloDisponibleDTO,
  ClienteDisponibleDTO,
  PaginaDTO,
  FacturaDetalleVistaDTO,
  FacturaOpcionesDTO,
  FacturaFiltrosDTO,
  FacturaReporteDTO,
  FacturaRequestDTO,
  FacturaVistaDTO,
  EntidadCreadaDTO,
} from "../models/factura.dto";

interface ApiError {
  mensaje?: string;
}

type JsonObject = Record<string, unknown>;

const facturaReporteShape = {
  id: "number",
  numeroFactura: "number",
  fechaEmision: "string",
  clienteDenominacion: "string",
  condicionIva: "string",
  puntoVentaDescripcion: "string",
  importeTotal: "number",
  cantidadItems: "number",
  estado: "string",
} as const satisfies Record<keyof FacturaReporteDTO, "number" | "string">;

function facturaReporteDTO(value: unknown): FacturaReporteDTO {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new Error("El backend no devolvió un FacturaReporteDTO válido");
  }

  const dto = value as JsonObject;
  const facturaReporteFields = Object.keys(facturaReporteShape) as (keyof FacturaReporteDTO)[];
  const missingFields = facturaReporteFields.filter((field) => !(field in dto));
  const unexpectedFields = Object.keys(dto).filter((field) => !(field in facturaReporteShape));
  if (missingFields.length > 0 || unexpectedFields.length > 0) {
    const differences = [
      missingFields.length ? `faltan: ${missingFields.join(", ")}` : "",
      unexpectedFields.length ? `sobran: ${unexpectedFields.join(", ")}` : "",
    ].filter(Boolean).join("; ");
    throw new Error(
      `FacturaReporteDTO no coincide entre Java y TypeScript (${differences}). Reiniciá Spring Boot.`,
    );
  }

  if (facturaReporteFields.some((field) => typeof dto[field] !== facturaReporteShape[field])) {
    throw new Error("Los tipos de FacturaReporteDTO no coinciden entre Java y TypeScript");
  }

  return dto as unknown as FacturaReporteDTO;
}

function facturaReporteListDTO(value: unknown): FacturaReporteDTO[] {
  if (!Array.isArray(value)) {
    throw new Error("El backend no devolvió una lista de FacturaReporteDTO");
  }
  return value.map(facturaReporteDTO);
}

const facturaDetalleVistaFields = [
  "id", "articuloId", "articuloCodigo", "articuloDenominacion",
  "listaPrecioArticuloId", "listaPrecio", "descripcion", "cantidad",
  "precioUnitario", "porcentajeBonificacion", "porcentajeIva",
  "importeNeto", "importeIva", "importeSubtotal",
] as const satisfies readonly (keyof FacturaDetalleVistaDTO)[];

const facturaVistaFields = [
  "id", "numero", "fechaEmision", "puntoVentaId", "puntoVentaDescripcion",
  "clienteId", "clienteDenominacion", "listaPrecioId", "listaPrecioCodigo", "listaPrecio",
  "condicionIvaId", "condicionIva",
  "tipoMonedaId", "tipoMoneda", "usuarioId", "usuario", "estado",
  "importeCobrado", "importeSaldo", "importeTotal", "observaciones",
  "fechaAnulacion", "detalles",
] as const satisfies readonly (keyof FacturaVistaDTO)[];

const articuloAltaOpcionesFields = [
  "rubros", "marcas", "listasPrecio", "usuario",
] as const satisfies readonly (keyof ArticuloAltaOpcionesDTO)[];

const articuloAltaOpcionFields = [
  "id", "descripcion",
] as const satisfies readonly (keyof ArticuloAltaOpcionDTO)[];

const articuloAltaListaFields = [
  "id", "codigo", "denominacion",
] as const satisfies readonly (keyof ArticuloAltaListaPrecioOpcionDTO)[];

function exactFields(value: unknown, fields: readonly string[], dtoName: string): JsonObject {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new Error(`El backend no devolvió un ${dtoName} válido`);
  }
  const dto = value as JsonObject;
  const missing = fields.filter((field) => !(field in dto));
  const unexpected = Object.keys(dto).filter((field) => !fields.includes(field));
  if (missing.length || unexpected.length) {
    const differences = [
      missing.length ? `faltan: ${missing.join(", ")}` : "",
      unexpected.length ? `sobran: ${unexpected.join(", ")}` : "",
    ].filter(Boolean).join("; ");
    throw new Error(`${dtoName} no coincide entre Java y TypeScript (${differences})`);
  }
  return dto;
}

function facturaVistaDTO(value: unknown): FacturaVistaDTO {
  const dto = exactFields(value, facturaVistaFields, "FacturaVistaDTO");
  if (!Array.isArray(dto.detalles)) {
    throw new Error("FacturaVistaDTO.detalles debe ser un arreglo");
  }
  dto.detalles.forEach((detail) =>
    exactFields(detail, facturaDetalleVistaFields, "FacturaDetalleVistaDTO")
  );
  return dto as unknown as FacturaVistaDTO;
}

function articuloAltaOpcionDTO(value: unknown): ArticuloAltaOpcionDTO {
  const dto = exactFields(value, articuloAltaOpcionFields, "ArticuloAltaDTO.Opcion");
  if (typeof dto.id !== "number" || typeof dto.descripcion !== "string") {
    throw new Error("Los tipos de ArticuloAltaDTO.Opcion no coinciden entre Java y TypeScript");
  }
  return dto as unknown as ArticuloAltaOpcionDTO;
}

function articuloAltaListaDTO(value: unknown): ArticuloAltaListaPrecioOpcionDTO {
  const dto = exactFields(value, articuloAltaListaFields, "ArticuloAltaDTO.ListaPrecioOpcion");
  if (typeof dto.id !== "number" || typeof dto.codigo !== "string"
      || typeof dto.denominacion !== "string") {
    throw new Error("Los tipos de ListaPrecioOpcion no coinciden entre Java y TypeScript");
  }
  return dto as unknown as ArticuloAltaListaPrecioOpcionDTO;
}

function articuloAltaOpcionesDTO(value: unknown): ArticuloAltaOpcionesDTO {
  const dto = exactFields(value, articuloAltaOpcionesFields, "ArticuloAltaDTO.Opciones");
  if (!Array.isArray(dto.rubros) || !Array.isArray(dto.marcas)
      || !Array.isArray(dto.listasPrecio)) {
    throw new Error("Las opciones de alta de artículo no contienen arreglos válidos");
  }
  dto.rubros.forEach(articuloAltaOpcionDTO);
  dto.marcas.forEach(articuloAltaOpcionDTO);
  dto.listasPrecio.forEach(articuloAltaListaDTO);
  if (dto.usuario !== null) articuloAltaOpcionDTO(dto.usuario);
  return dto as unknown as ArticuloAltaOpcionesDTO;
}

function entidadCreadaDTO(value: unknown): EntidadCreadaDTO {
  const dto = exactFields(value, ["id", "descripcion"], "EntidadCreadaDTO");
  if (typeof dto.id !== "number" || typeof dto.descripcion !== "string") {
    throw new Error("Los tipos de EntidadCreadaDTO no coinciden entre Java y TypeScript");
  }
  return dto as unknown as EntidadCreadaDTO;
}

// CONSIGNA ESCRITURA - 4.b y 6.a: cliente HTTP centralizado para la API REST.
export class FacturaService {
  private readonly url = "http://localhost:8080/api/facturas";
  private readonly datosMaestrosUrl = "http://localhost:8080/api/datos-maestros";

  async buscarArticulosDisponibles(
    busqueda: string,
    pagina: number,
    tamanio: number,
    listaPrecioId: number,
  ): Promise<PaginaDTO<ArticuloDisponibleDTO>> {
    const parametros = new URLSearchParams({
      busqueda,
      pagina: String(pagina),
      tamanio: String(tamanio),
      listaPrecioId: String(listaPrecioId),
    });
    return this.solicitar<PaginaDTO<ArticuloDisponibleDTO>>(
      `${this.datosMaestrosUrl}/precios-articulo?${parametros.toString()}`,
    );
  }

  async buscarClientesDisponibles(
    busqueda: string,
    pagina: number,
    tamanio: number,
    orden: string,
  ): Promise<PaginaDTO<ClienteDisponibleDTO>> {
    const parametros = new URLSearchParams({
      busqueda,
      pagina: String(pagina),
      tamanio: String(tamanio),
      orden,
    });
    return this.solicitar<PaginaDTO<ClienteDisponibleDTO>>(
      `${this.datosMaestrosUrl}/clientes-disponibles?${parametros.toString()}`,
    );
  }

  async obtenerOpcionesFactura(): Promise<FacturaOpcionesDTO> {
    return this.solicitar<FacturaOpcionesDTO>(`${this.datosMaestrosUrl}/opciones-factura`);
  }

  async obtenerOpcionesAltaArticulo(): Promise<ArticuloAltaOpcionesDTO> {
    const response = await this.solicitar<unknown>(`${this.datosMaestrosUrl}/articulos/opciones`);
    return articuloAltaOpcionesDTO(response);
  }

  async crearArticuloConPrecios(
    articulo: ArticuloAltaRequestDTO,
  ): Promise<EntidadCreadaDTO> {
    const response = await this.solicitar<unknown>(
      `${this.datosMaestrosUrl}/articulos-con-precios`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(articulo),
      },
    );
    return entidadCreadaDTO(response);
  }

  // CONSIGNA ESCRITURA - 6.b: GET con filtros opcionales.
  async obtenerFacturas(filtros: FacturaFiltrosDTO): Promise<FacturaReporteDTO[]> {
    const parametros = new URLSearchParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== "") parametros.set(clave, String(valor));
    });
    const query = parametros.size ? `?${parametros.toString()}` : "";
    return facturaReporteListDTO(await this.solicitar<unknown>(`${this.url}${query}`));
  }

  async obtenerFactura(id: number): Promise<FacturaVistaDTO> {
    return facturaVistaDTO(await this.solicitar<unknown>(`${this.url}/${id}`));
  }

  // CONSIGNA ESCRITURA - 6.b: POST de una factura completa.
  async crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    const response = await this.solicitar<unknown>(this.url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(factura),
    });
    return facturaReporteDTO(response);
  }

  async actualizarFactura(id: number, factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    const response = await this.solicitar<unknown>(`${this.url}/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(factura),
    });
    return facturaReporteDTO(response);
  }

  // CONSIGNA ESCRITURA - 6.b: PUT parcial para anular o cambiar el estado.
  async actualizarEstado(
    id: number,
    estado: string,
    observaciones?: string,
  ): Promise<FacturaReporteDTO> {
    const cambios: Partial<FacturaRequestDTO> = { estado, observaciones };
    const response = await this.solicitar<unknown>(`${this.url}/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(cambios),
    });
    return facturaReporteDTO(response);
  }

  // CONSIGNA ESCRITURA - 6.b: DELETE sin cuerpo de respuesta.
  async eliminarFactura(id: number): Promise<void> {
    await this.solicitar<void>(`${this.url}/${id}`, { method: "DELETE" });
  }

  private async solicitar<T>(url: string, opciones?: RequestInit): Promise<T> {
    const respuesta = await fetch(url, opciones);
    if (!respuesta.ok) {
      const error = await respuesta.json().catch(() => ({})) as ApiError;
      throw new Error(error.mensaje ?? `Error HTTP ${respuesta.status}`);
    }
    if (respuesta.status === 204) return undefined as T;
    return await respuesta.json() as T;
  }
}
