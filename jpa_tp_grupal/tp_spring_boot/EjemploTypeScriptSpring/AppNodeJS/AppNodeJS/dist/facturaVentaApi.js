var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { registrarPeticion } from './apiLogger.js';
const API_BASE_URL = 'http://localhost:8080/api/facturas';
function manejarRespuesta(respuesta) {
    return __awaiter(this, void 0, void 0, function* () {
        if (!respuesta.ok) {
            const mensaje = yield respuesta.text();
            throw new Error(`Error ${respuesta.status}: ${mensaje || respuesta.statusText}`);
        }
        if (respuesta.status === 204) {
            return undefined;
        }
        return respuesta.json();
    });
}
function construirQueryString(filtros) {
    const params = new URLSearchParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
        if (valor !== undefined && valor !== null && valor.trim() !== '') {
            params.append(clave, valor);
        }
    });
    const query = params.toString();
    return query ? `?${query}` : '';
}
export function obtenerFacturas() {
    return __awaiter(this, arguments, void 0, function* (filtros = {}) {
        const url = `${API_BASE_URL}${construirQueryString(filtros)}`;
        registrarPeticion({ metodo: 'GET', url });
        const respuesta = yield fetch(url);
        return manejarRespuesta(respuesta);
    });
}
export function obtenerFactura(numeroFactura) {
    return __awaiter(this, void 0, void 0, function* () {
        const url = `${API_BASE_URL}/${numeroFactura}`;
        registrarPeticion({ metodo: 'GET', url });
        const respuesta = yield fetch(url);
        return manejarRespuesta(respuesta);
    });
}
export function crearFactura(factura) {
    return __awaiter(this, void 0, void 0, function* () {
        const headers = { 'Content-Type': 'application/json' };
        const body = JSON.stringify(factura);
        registrarPeticion({ metodo: 'POST', url: API_BASE_URL, headers, body: factura });
        const respuesta = yield fetch(API_BASE_URL, {
            method: 'POST',
            headers,
            body
        });
        return manejarRespuesta(respuesta);
    });
}
export function actualizarFactura(numeroFactura, factura) {
    return __awaiter(this, void 0, void 0, function* () {
        const url = `${API_BASE_URL}/${numeroFactura}`;
        const headers = { 'Content-Type': 'application/json' };
        const body = JSON.stringify(factura);
        registrarPeticion({ metodo: 'PUT', url, headers, body: factura });
        const respuesta = yield fetch(url, {
            method: 'PUT',
            headers,
            body
        });
        return manejarRespuesta(respuesta);
    });
}
export function eliminarFactura(numeroFactura) {
    return __awaiter(this, void 0, void 0, function* () {
        const url = `${API_BASE_URL}/${numeroFactura}`;
        registrarPeticion({ metodo: 'DELETE', url });
        const respuesta = yield fetch(url, {
            method: 'DELETE'
        });
        return manejarRespuesta(respuesta);
    });
}
