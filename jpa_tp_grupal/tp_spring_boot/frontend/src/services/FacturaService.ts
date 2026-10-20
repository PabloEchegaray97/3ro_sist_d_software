import type {
  ArticuloDisponibleDTO,
  FacturaOpcionesDTO,
  FacturaFiltrosDTO,
  FacturaReporteDTO,
  FacturaRequestDTO,
} from "../models/factura.dto";

interface ApiError {
  mensaje?: string;
}

// CONSIGNA ESCRITURA - 4.b y 6.a: cliente HTTP centralizado para la API REST.
export class FacturaService {
  private readonly url = "http://localhost:8080/api/facturas";
  private readonly datosMaestrosUrl = "http://localhost:8080/api/datos-maestros";

  async obtenerArticulosDisponibles(): Promise<ArticuloDisponibleDTO[]> {
    return this.solicitar<ArticuloDisponibleDTO[]>(`${this.datosMaestrosUrl}/precios-articulo`);
  }

  async obtenerOpcionesFactura(): Promise<FacturaOpcionesDTO> {
    return this.solicitar<FacturaOpcionesDTO>(`${this.datosMaestrosUrl}/opciones-factura`);
  }

  // CONSIGNA ESCRITURA - 6.b: GET con filtros opcionales.
  async obtenerFacturas(filtros: FacturaFiltrosDTO): Promise<FacturaReporteDTO[]> {
    const parametros = new URLSearchParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== "") parametros.set(clave, String(valor));
    });
    const query = parametros.size ? `?${parametros.toString()}` : "";
    return this.solicitar<FacturaReporteDTO[]>(`${this.url}${query}`);
  }

  // CONSIGNA ESCRITURA - 6.b: POST de una factura completa.
  async crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    return this.solicitar<FacturaReporteDTO>(this.url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(factura),
    });
  }

  // CONSIGNA ESCRITURA - 6.b: PUT parcial para anular o cambiar el estado.
  async actualizarEstado(
    id: number,
    estado: string,
    observaciones?: string,
  ): Promise<FacturaReporteDTO> {
    const cambios: Partial<FacturaRequestDTO> = { estado, observaciones };
    return this.solicitar<FacturaReporteDTO>(`${this.url}/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(cambios),
    });
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
    return respuesta.json() as Promise<T>;
  }
}
