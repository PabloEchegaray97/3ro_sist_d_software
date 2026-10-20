export function calcularSubtotal(detalle) {
    var _a, _b;
    const cantidad = (_a = detalle.cantidad) !== null && _a !== void 0 ? _a : 0;
    const precio = (_b = detalle.precioUnitario) !== null && _b !== void 0 ? _b : 0;
    return cantidad * precio;
}
export function calcularImporteTotal(factura) {
    return factura.detalles.reduce((total, detalle) => total + calcularSubtotal(detalle), 0);
}
export function crearDetalleVacio(id) {
    return {
        id,
        productoDescripcion: '',
        cantidad: 1,
        precioUnitario: 0,
        subtotal: 0
    };
}
export function crearFacturaVacia() {
    const hoy = new Date().toISOString().split('T')[0];
    return {
        fechaEmision: hoy,
        clienteDenominacion: '',
        condicionIva: '',
        puntoVentaDescripcion: '',
        detalles: [crearDetalleVacio()]
    };
}
export function esNumeroFacturaValido(numeroFactura) {
    return numeroFactura !== null && numeroFactura !== undefined && numeroFactura.trim() !== '';
}
