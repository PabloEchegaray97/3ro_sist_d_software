import { eliminarFactura, FiltrosFactura, obtenerFacturas } from './facturaVentaApi.js';
import { FacturaVentaDTO } from './facturaVentaDTO.js';

const tbody = document.getElementById('facturas-tbody') as HTMLTableSectionElement;
const filtrosForm = document.getElementById('filtros-form') as HTMLFormElement;
const btnLimpiar = document.getElementById('btn-limpiar') as HTMLButtonElement;

function formatearFecha(fechaStr: string | undefined): string {
    if (!fechaStr) return '-';
    const fecha = new Date(fechaStr);
    if (isNaN(fecha.getTime())) return fechaStr;
    return fecha.toLocaleDateString('es-AR');
}

function formatearMoneda(valor: number | undefined): string {
    if (valor === undefined || valor === null) return '0.00';
    return valor.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function crearBoton(texto: string, clases: string, onClick: () => void): HTMLButtonElement {
    const boton = document.createElement('button');
    boton.textContent = texto;
    boton.className = `btn btn-sm ${clases}`;
    boton.addEventListener('click', onClick);
    return boton;
}

function renderizarFacturas(facturas: FacturaVentaDTO[]): void {
    tbody.innerHTML = '';

    if (facturas.length === 0) {
        const fila = document.createElement('tr');
        fila.innerHTML = '<td colspan="8" class="text-center">No se encontraron facturas.</td>';
        tbody.appendChild(fila);
        return;
    }

    facturas.forEach(factura => {
        const fila = document.createElement('tr');
        fila.innerHTML = `
            <td>${factura.numeroFactura ?? '-'}</td>
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

        grupoBotones.appendChild(crearBoton('Eliminar', 'btn-outline-danger', async () => {
            if (factura.numeroFactura === undefined) return;
            if (!confirm(`¿Eliminar la factura ${factura.numeroFactura}?`)) return;
            try {
                await eliminarFactura(factura.numeroFactura);
                await cargarFacturas();
            } catch (error) {
                alert('Error al eliminar la factura: ' + (error as Error).message);
            }
        }));

        celdaAcciones.appendChild(grupoBotones);
        tbody.appendChild(fila);
    });
}

async function cargarFacturas(): Promise<void> {
    try {
        const filtros = leerFiltros();
        const facturas = await obtenerFacturas(filtros);
        renderizarFacturas(facturas);
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger">Error al cargar facturas: ${(error as Error).message}</td></tr>`;
    }
}

function leerFiltros(): FiltrosFactura {
    return {
        numeroFactura: (document.getElementById('filtro-numero') as HTMLInputElement).value,
        clienteDenominacion: (document.getElementById('filtro-cliente') as HTMLInputElement).value,
        condicionIva: (document.getElementById('filtro-condicion') as HTMLInputElement).value,
        fechaDesde: (document.getElementById('filtro-fecha-desde') as HTMLInputElement).value,
        fechaHasta: (document.getElementById('filtro-fecha-hasta') as HTMLInputElement).value
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
