var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { eliminarFactura, obtenerFacturas } from './facturaVentaApi.js';
const tbody = document.getElementById('facturas-tbody');
const filtrosForm = document.getElementById('filtros-form');
const btnLimpiar = document.getElementById('btn-limpiar');
function formatearFecha(fechaStr) {
    if (!fechaStr)
        return '-';
    const fecha = new Date(fechaStr);
    if (isNaN(fecha.getTime()))
        return fechaStr;
    return fecha.toLocaleDateString('es-AR');
}
function formatearMoneda(valor) {
    if (valor === undefined || valor === null)
        return '0.00';
    return valor.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function crearBoton(texto, clases, onClick) {
    const boton = document.createElement('button');
    boton.textContent = texto;
    boton.className = `btn btn-sm ${clases}`;
    boton.addEventListener('click', onClick);
    return boton;
}
function renderizarFacturas(facturas) {
    tbody.innerHTML = '';
    if (facturas.length === 0) {
        const fila = document.createElement('tr');
        fila.innerHTML = '<td colspan="8" class="text-center">No se encontraron facturas.</td>';
        tbody.appendChild(fila);
        return;
    }
    facturas.forEach(factura => {
        var _a;
        const fila = document.createElement('tr');
        fila.innerHTML = `
            <td>${(_a = factura.numeroFactura) !== null && _a !== void 0 ? _a : '-'}</td>
            <td>${formatearFecha(factura.fechaEmision)}</td>
            <td>${factura.clienteDenominacion}</td>
            <td>${factura.condicionIva}</td>
            <td>${factura.puntoVentaDescripcion}</td>
            <td class="text-end">${formatearMoneda(factura.importeTotal)}</td>
            <td class="text-center">${factura.detalles.length}</td>
            <td class="text-center"></td>
        `;
        const celdaAcciones = fila.cells[fila.cells.length - 1];
        const grupoBotones = document.createElement('div');
        grupoBotones.className = 'btn-group';
        grupoBotones.appendChild(crearBoton('Editar', 'btn-outline-primary', () => {
            if (factura.numeroFactura !== undefined) {
                window.location.href = `/factura-form.html?numero=${factura.numeroFactura}`;
            }
        }));
        grupoBotones.appendChild(crearBoton('Eliminar', 'btn-outline-danger', () => __awaiter(this, void 0, void 0, function* () {
            if (factura.numeroFactura === undefined)
                return;
            if (!confirm(`¿Eliminar la factura ${factura.numeroFactura}?`))
                return;
            try {
                yield eliminarFactura(factura.numeroFactura);
                yield cargarFacturas();
            }
            catch (error) {
                alert('Error al eliminar la factura: ' + error.message);
            }
        })));
        celdaAcciones.appendChild(grupoBotones);
        tbody.appendChild(fila);
    });
}
function cargarFacturas() {
    return __awaiter(this, void 0, void 0, function* () {
        try {
            const filtros = leerFiltros();
            const facturas = yield obtenerFacturas(filtros);
            renderizarFacturas(facturas);
        }
        catch (error) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger">Error al cargar facturas: ${error.message}</td></tr>`;
        }
    });
}
function leerFiltros() {
    return {
        numeroFactura: document.getElementById('filtro-numero').value,
        clienteDenominacion: document.getElementById('filtro-cliente').value,
        condicionIva: document.getElementById('filtro-condicion').value,
        fechaDesde: document.getElementById('filtro-fecha-desde').value,
        fechaHasta: document.getElementById('filtro-fecha-hasta').value
    };
}
filtrosForm.addEventListener('submit', (evento) => {
    evento.preventDefault();
    cargarFacturas();
});
btnLimpiar.addEventListener('click', () => {
    filtrosForm.reset();
    cargarFacturas();
});
cargarFacturas();
