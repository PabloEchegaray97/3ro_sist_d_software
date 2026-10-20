export interface FacturaVentaDetalleDTO {
    id?: number;
    productoDescripcion: string;
    cantidad: number;
    precioUnitario: number;
    subtotal?: number;
}

export interface FacturaVentaDTO {
    numeroFactura?: number;
    fechaEmision: string;
    clienteDenominacion: string;
    condicionIva: string;
    puntoVentaDescripcion: string;
    importeTotal?: number;
    detalles: FacturaVentaDetalleDTO[];
}

export function calcularSubtotal(detalle: FacturaVentaDetalleDTO): number {
    const cantidad = detalle.cantidad ?? 0;
    const precio = detalle.precioUnitario ?? 0;
    return cantidad * precio;
}

export function calcularImporteTotal(factura: FacturaVentaDTO): number {
    return factura.detalles.reduce((total, detalle) => total + calcularSubtotal(detalle), 0);
}

export function crearDetalleVacio(id?: number): FacturaVentaDetalleDTO {
    return {
        id,
        productoDescripcion: '',
        cantidad: 1,
        precioUnitario: 0,
        subtotal: 0
    };
}

export function crearFacturaVacia(): FacturaVentaDTO {
    const hoy = new Date().toISOString().split('T')[0];
    return {
        fechaEmision: hoy,
        clienteDenominacion: '',
        condicionIva: '',
        puntoVentaDescripcion: '',
        detalles: [crearDetalleVacio()]
    };
}

export function esNumeroFacturaValido(numeroFactura: string | null | undefined): numeroFactura is string {
    return numeroFactura !== null && numeroFactura !== undefined && numeroFactura.trim() !== '';
}
