var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { actualizarFactura, crearFactura, obtenerFactura } from './facturaVentaApi.js';
import { calcularImporteTotal, calcularSubtotal, crearDetalleVacio, crearFacturaVacia, esNumeroFacturaValido } from './facturaVentaDTO.js';
const form = document.getElementById('factura-form');
const numeroFacturaInput = document.getElementById('numero-factura');
const fechaEmisionInput = document.getElementById('fecha-emision');
const puntoVentaInput = document.getElementById('punto-venta');
const clienteInput = document.getElementById('cliente');
const condicionIvaInput = document.getElementById('condicion-iva');
const detallesTbody = document.getElementById('detalles-tbody');
const importeTotalCell = document.getElementById('importe-total');
const btnAgregarDetalle = document.getElementById('btn-agregar-detalle');
const titulo = document.getElementById('titulo');
const params = new URLSearchParams(window.location.search);
const numeroFacturaParam = params.get('numero');
const modoEdicion = esNumeroFacturaValido(numeroFacturaParam);
let detalles = [];
let siguienteDetalleId = 1;
function formatearMoneda(valor) {
    if (valor != undefined)
        return valor.toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return "0";
}
function actualizarImporteTotal() {
    const factura = {
        fechaEmision: '',
        clienteDenominacion: '',
        condicionIva: '',
        puntoVentaDescripcion: '',
        detalles
    };
    importeTotalCell.textContent = formatearMoneda(calcularImporteTotal(factura));
}
function crearInputNumber(valor, onChange) {
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
function crearFilaDetalle(detalle, indice) {
    var _a;
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
    celdaSubtotal.textContent = formatearMoneda((_a = detalle.subtotal) !== null && _a !== void 0 ? _a : calcularSubtotal(detalle));
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
function renderizarDetalles() {
    detallesTbody.innerHTML = '';
    detalles.forEach((detalle, indice) => {
        detallesTbody.appendChild(crearFilaDetalle(detalle, indice));
    });
}
function agregarDetalle() {
    detalles.push(crearDetalleVacio(siguienteDetalleId++));
    renderizarDetalles();
    actualizarImporteTotal();
}
function cargarFacturaEnFormulario(factura) {
    var _a, _b;
    numeroFacturaInput.value = (_b = (_a = factura.numeroFactura) === null || _a === void 0 ? void 0 : _a.toString()) !== null && _b !== void 0 ? _b : '';
    fechaEmisionInput.value = factura.fechaEmision;
    puntoVentaInput.value = factura.puntoVentaDescripcion;
    clienteInput.value = factura.clienteDenominacion;
    condicionIvaInput.value = factura.condicionIva;
    detalles = factura.detalles.map((detalle, indice) => {
        var _a, _b;
        return (Object.assign(Object.assign({}, detalle), { id: (_a = detalle.id) !== null && _a !== void 0 ? _a : (indice + 1), subtotal: (_b = detalle.subtotal) !== null && _b !== void 0 ? _b : calcularSubtotal(detalle) }));
    });
    siguienteDetalleId = detalles.length > 0
        ? Math.max(...detalles.map(d => { var _a; return (_a = d.id) !== null && _a !== void 0 ? _a : 0; })) + 1
        : 1;
    renderizarDetalles();
    actualizarImporteTotal();
}
function leerFacturaDesdeFormulario() {
    const factura = {
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
function cargarFacturaExistente() {
    return __awaiter(this, void 0, void 0, function* () {
        if (!modoEdicion)
            return;
        titulo.textContent = 'Editar Factura de Venta';
        try {
            const numeroFactura = parseInt(numeroFacturaParam, 10);
            const factura = yield obtenerFactura(numeroFactura);
            cargarFacturaEnFormulario(factura);
        }
        catch (error) {
            alert('Error al cargar la factura: ' + error.message);
        }
    });
}
btnAgregarDetalle.addEventListener('click', agregarDetalle);
form.addEventListener('submit', (evento) => __awaiter(void 0, void 0, void 0, function* () {
    evento.preventDefault();
    if (detalles.length === 0) {
        alert('Debe agregar al menos un detalle.');
        return;
    }
    const factura = leerFacturaDesdeFormulario();
    try {
        if (modoEdicion) {
            yield actualizarFactura(factura.numeroFactura, factura);
        }
        else {
            yield crearFactura(factura);
        }
        window.location.href = '/index.html';
    }
    catch (error) {
        alert('Error al guardar la factura: ' + error.message);
    }
}));
if (modoEdicion) {
    cargarFacturaExistente();
}
else {
    cargarFacturaEnFormulario(crearFacturaVacia());
}
