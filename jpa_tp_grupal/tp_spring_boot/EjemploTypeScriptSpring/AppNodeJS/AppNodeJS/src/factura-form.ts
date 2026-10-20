import { actualizarFactura, crearFactura, obtenerFactura } from './facturaVentaApi.js';
import {
    calcularImporteTotal,
    calcularSubtotal,
    crearDetalleVacio,
    crearFacturaVacia,
    esNumeroFacturaValido,
    FacturaVentaDTO,
    FacturaVentaDetalleDTO
} from './facturaVentaDTO.js';

const form = document.getElementById('factura-form') as HTMLFormElement;
const numeroFacturaInput = document.getElementById('numero-factura') as HTMLInputElement;
const fechaEmisionInput = document.getElementById('fecha-emision') as HTMLInputElement;
const puntoVentaInput = document.getElementById('punto-venta') as HTMLInputElement;
const clienteInput = document.getElementById('cliente') as HTMLInputElement;
const condicionIvaInput = document.getElementById('condicion-iva') as HTMLInputElement;
const detallesTbody = document.getElementById('detalles-tbody') as HTMLTableSectionElement;
const importeTotalCell = document.getElementById('importe-total') as HTMLTableCellElement;
const btnAgregarDetalle = document.getElementById('btn-agregar-detalle') as HTMLButtonElement;
const titulo = document.getElementById('titulo') as HTMLHeadingElement;

const params = new URLSearchParams(window.location.search);
const numeroFacturaParam = params.get('numero');
const modoEdicion = esNumeroFacturaValido(numeroFacturaParam);

let detalles: FacturaVentaDetalleDTO[] = [];
let siguienteDetalleId = 1;

function formatearMoneda(valor: number | undefined): string {
    if(valor != undefined)
        return valor.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return "0";
}

function actualizarImporteTotal(): void {
    const factura: FacturaVentaDTO = {
        fechaEmision: '',
        clienteDenominacion: '',
        condicionIva: '',
        puntoVentaDescripcion: '',
        detalles
    };
    importeTotalCell.textContent = formatearMoneda(calcularImporteTotal(factura));
}

function crearInputNumber(valor: number, onChange: (valor: number) => void): HTMLInputElement {
    const input = document.createElement('input');
    input.type = 'number';
    input.className = 'form-control';
    input.step = '0.01';
    input.min = '0';
    input.value = valor.toString();
    input.required = true;
    input.addEventListener('input', () => {
        const nuevoValor = parseFloat(input.value);
        onChange(isNaN(nuevoValor) ? 0 : nuevoValor);
        actualizarImporteTotal();
    });
    return input;
}

function crearFilaDetalle(detalle: FacturaVentaDetalleDTO, indice: number): HTMLTableRowElement {
    const fila = document.createElement('tr');

    const celdaProducto = document.createElement('td');
    const inputProducto = document.createElement('input');
    inputProducto.type = 'text';
    inputProducto.className = 'form-control';
    inputProducto.value = detalle.productoDescripcion;
    inputProducto.required = true;
    inputProducto.addEventListener('input', () => {
        detalles[indice].productoDescripcion = inputProducto.value;
    });
    celdaProducto.appendChild(inputProducto);

    const celdaCantidad = document.createElement('td');
    const inputCantidad = crearInputNumber(detalle.cantidad, (valor) => {
        detalles[indice].cantidad = valor;
        detalles[indice].subtotal = calcularSubtotal(detalles[indice]);
        const subtotalCell = fila.cells[3];
        subtotalCell.textContent = formatearMoneda(detalles[indice].subtotal);
    });
    inputCantidad.step = '1';
    celdaCantidad.appendChild(inputCantidad);

    const celdaPrecio = document.createElement('td');
    const inputPrecio = crearInputNumber(detalle.precioUnitario, (valor) => {
        detalles[indice].precioUnitario = valor;
        detalles[indice].subtotal = calcularSubtotal(detalles[indice]);
        const subtotalCell = fila.cells[3];
        subtotalCell.textContent = formatearMoneda(detalles[indice].subtotal);
    });
    celdaPrecio.appendChild(inputPrecio);

    const celdaSubtotal = document.createElement('td');
    celdaSubtotal.textContent = formatearMoneda(detalle.subtotal ?? calcularSubtotal(detalle));

    const celdaAcciones = document.createElement('td');
    const btnEliminar = document.createElement('button');
    btnEliminar.type = 'button';
    btnEliminar.className = 'btn btn-outline-danger btn-sm';
    btnEliminar.textContent = 'Eliminar';
    btnEliminar.addEventListener('click', () => {
        detalles.splice(indice, 1);
        renderizarDetalles();
        actualizarImporteTotal();
    });
    celdaAcciones.appendChild(btnEliminar);

    fila.appendChild(celdaProducto);
    fila.appendChild(celdaCantidad);
    fila.appendChild(celdaPrecio);
    fila.appendChild(celdaSubtotal);
    fila.appendChild(celdaAcciones);

    return fila;
}

function renderizarDetalles(): void {
    detallesTbody.innerHTML = '';
    detalles.forEach((detalle, indice) => {
        detallesTbody.appendChild(crearFilaDetalle(detalle, indice));
    });
}

function agregarDetalle(): void {
    detalles.push(crearDetalleVacio(siguienteDetalleId++));
    renderizarDetalles();
    actualizarImporteTotal();
}

function cargarFacturaEnFormulario(factura: FacturaVentaDTO): void {
    numeroFacturaInput.value = factura.numeroFactura?.toString() ?? '';
    fechaEmisionInput.value = factura.fechaEmision;
    puntoVentaInput.value = factura.puntoVentaDescripcion;
    clienteInput.value = factura.clienteDenominacion;
    condicionIvaInput.value = factura.condicionIva;

    detalles = factura.detalles.map((detalle, indice) => ({
        ...detalle,
        id: detalle.id ?? (indice + 1),
        subtotal: detalle.subtotal ?? calcularSubtotal(detalle)
    }));
    siguienteDetalleId = detalles.length > 0
        ? Math.max(...detalles.map(d => d.id ?? 0)) + 1
        : 1;
    renderizarDetalles();
    actualizarImporteTotal();
}

function leerFacturaDesdeFormulario(): FacturaVentaDTO {
    const factura: FacturaVentaDTO = {
        fechaEmision: fechaEmisionInput.value,
        clienteDenominacion: clienteInput.value,
        condicionIva: condicionIvaInput.value,
        puntoVentaDescripcion: puntoVentaInput.value,
        detalles: detalles.map(detalle => ({
            id: detalle.id,
            productoDescripcion: detalle.productoDescripcion,
            cantidad: detalle.cantidad,
            precioUnitario: detalle.precioUnitario,
            subtotal: calcularSubtotal(detalle)
        }))
    };
    factura.importeTotal = calcularImporteTotal(factura);

    if (modoEdicion) {
        factura.numeroFactura = parseInt(numeroFacturaParam, 10);
    }

    return factura;
}

async function cargarFacturaExistente(): Promise<void> {
    if (!modoEdicion) return;
    titulo.textContent = 'Editar Factura de Venta';
    try {
        const numeroFactura = parseInt(numeroFacturaParam, 10);
        const factura = await obtenerFactura(numeroFactura);
        cargarFacturaEnFormulario(factura);
    } catch (error) {
        alert('Error al cargar la factura: ' + (error as Error).message);
    }
}

btnAgregarDetalle.addEventListener('click', agregarDetalle);

form.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    if (detalles.length === 0) {
        alert('Debe agregar al menos un detalle.');
        return;
    }

    const factura = leerFacturaDesdeFormulario();

    try {
        if (modoEdicion) {
            await actualizarFactura(factura.numeroFactura!, factura);
        } else {
            await crearFactura(factura);
        }
        window.location.href = '/index.html';
    } catch (error) {
        alert('Error al guardar la factura: ' + (error as Error).message);
    }
});

if (modoEdicion) {
    cargarFacturaExistente();
} else {
    cargarFacturaEnFormulario(crearFacturaVacia());
}
