import { FacturaVentaDTO } from './facturaVentaDTO.js';
import { registrarPeticion } from './apiLogger.js';

const API_BASE_URL = 'http://localhost:8080/api/facturas';

export interface FiltrosFactura {
    numeroFactura?: string;
    clienteDenominacion?: string;
    condicionIva?: string;
    fechaDesde?: string;
    fechaHasta?: string;
}

async function manejarRespuesta<T>(respuesta: Response): Promise<T> {
    if (!respuesta.ok) {
        const mensaje = await respuesta.text();
        throw new Error(`Error ${respuesta.status}: ${mensaje || respuesta.statusText}`);
    }
    if (respuesta.status === 204) {
        return undefined as T;
    }
    return respuesta.json() as Promise<T>;
}

function construirQueryString(filtros: FiltrosFactura): string {
    const params = new URLSearchParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
        if (valor !== undefined && valor !== null && valor.trim() !== '') {
            params.append(clave, valor);
        }
    });
    const query = params.toString();
    return query ? `?${query}` : '';
}

export async function obtenerFacturas(filtros: FiltrosFactura = {}): Promise<FacturaVentaDTO[]> {
    const url = `${API_BASE_URL}${construirQueryString(filtros)}`;
    registrarPeticion({ metodo: 'GET', url });
    const respuesta = await fetch(url);
    return manejarRespuesta<FacturaVentaDTO[]>(respuesta);
}

export async function obtenerFactura(numeroFactura: number): Promise<FacturaVentaDTO> {
    const url = `${API_BASE_URL}/${numeroFactura}`;
    registrarPeticion({ metodo: 'GET', url });
    const respuesta = await fetch(url);
    return manejarRespuesta<FacturaVentaDTO>(respuesta);
}

export async function crearFactura(factura: FacturaVentaDTO): Promise<FacturaVentaDTO> {
    const headers = { 'Content-Type': 'application/json' };
    const body = JSON.stringify(factura);
    registrarPeticion({ metodo: 'POST', url: API_BASE_URL, headers, body: factura });
    const respuesta = await fetch(API_BASE_URL, {
        method: 'POST',
        headers,
        body
    });
    return manejarRespuesta<FacturaVentaDTO>(respuesta);
}

export async function actualizarFactura(numeroFactura: number, factura: FacturaVentaDTO): Promise<FacturaVentaDTO> {
    const url = `${API_BASE_URL}/${numeroFactura}`;
    const headers = { 'Content-Type': 'application/json' };
    const body = JSON.stringify(factura);
    registrarPeticion({ metodo: 'PUT', url, headers, body: factura });
    const respuesta = await fetch(url, {
        method: 'PUT',
        headers,
        body
    });
    return manejarRespuesta<FacturaVentaDTO>(respuesta);
}

export async function eliminarFactura(numeroFactura: number): Promise<void> {
    const url = `${API_BASE_URL}/${numeroFactura}`;
    registrarPeticion({ metodo: 'DELETE', url });
    const respuesta = await fetch(url, {
        method: 'DELETE'
    });
    return manejarRespuesta<void>(respuesta);
}
