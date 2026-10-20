package com.example.demo.service;

import com.example.demo.dto.FacturaReporteDTO;
import com.example.demo.dto.FacturaCreacionDTO;
import com.example.demo.dto.FacturaCreadaDTO;
import com.example.demo.dto.FacturaDetalleCreacionDTO;
import com.example.demo.model.Cliente;
import com.example.demo.model.CondicionIva;
import com.example.demo.model.FacturaVenta;
import com.example.demo.model.FacturaVentaDetalle;
import com.example.demo.model.ListaPrecioArticulo;
import com.example.demo.model.PuntoVenta;
import com.example.demo.model.TipoMoneda;
import com.example.demo.model.Usuario;
import com.example.demo.repository.FacturaEscrituraRepository;
import com.example.demo.repository.FacturaReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaReporteRepository facturaReporteRepository;
    private final FacturaEscrituraRepository facturaEscrituraRepository;

    @Transactional(readOnly = true)
    // CONSIGNA - PARTE 3.2: el servicio devuelve List<FacturaReporteDTO>
    // y delega en la consulta JPQL dinamica del repositorio.
    public List<FacturaReporteDTO> buscarFacturasFiltradas(
            Date fechaDesde,
            Date fechaHasta,
            String estado,
            Double montoMinimo
    ) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.after(fechaHasta)) {
            throw new IllegalArgumentException("fechaDesde no puede ser posterior a fechaHasta");
        }
        if (montoMinimo != null && montoMinimo < 0) {
            throw new IllegalArgumentException("montoMinimo no puede ser negativo");
        }

        return facturaReporteRepository.buscarFacturasFiltradas(
                fechaDesde,
                fechaHasta,
                estado,
                montoMinimo
        );
    }

    @Transactional
    public FacturaCreadaDTO crearFactura(FacturaCreacionDTO solicitud) {
        validarSolicitud(solicitud);

        if (facturaEscrituraRepository.existeNumero(solicitud.getNumero())) {
            throw new IllegalArgumentException("Ya existe una factura con el numero " + solicitud.getNumero());
        }

        Usuario usuario = facturaEscrituraRepository.buscarRequerido(
                Usuario.class, solicitud.getUsuarioId(), "usuarioId"
        );
        PuntoVenta puntoVenta = facturaEscrituraRepository.buscarRequerido(
                PuntoVenta.class, solicitud.getPuntoVentaId(), "puntoVentaId"
        );
        CondicionIva condicionIva = facturaEscrituraRepository.buscarRequerido(
                CondicionIva.class, solicitud.getCondicionIvaId(), "condicionIvaId"
        );
        TipoMoneda tipoMoneda = facturaEscrituraRepository.buscarRequerido(
                TipoMoneda.class, solicitud.getTipoMonedaId(), "tipoMonedaId"
        );
        Cliente cliente = solicitud.getClienteId() == null
                ? null
                : facturaEscrituraRepository.buscarRequerido(
                        Cliente.class, solicitud.getClienteId(), "clienteId"
                );

        java.util.Date fechaEmision = java.sql.Date.valueOf(solicitud.getFechaEmision());
        FacturaVenta factura = new FacturaVenta();
        factura.setNumero(solicitud.getNumero());
        factura.setFechaEmision(fechaEmision);
        factura.setPuntoVenta(puntoVenta);
        factura.setCliente(cliente);
        factura.setCondicionIva(condicionIva);
        factura.setTipoMoneda(tipoMoneda);
        factura.setEstado(solicitud.getEstado().trim().toUpperCase());
        factura.setObservaciones(solicitud.getObservaciones());
        factura.setFechaAlta(new java.util.Date());
        factura.setFechaModificacion(new java.util.Date());
        factura.setUsuarioCarga(usuario);
        factura.setUsuarioModificacion(usuario);

        double importeTotal = 0;
        for (FacturaDetalleCreacionDTO item : solicitud.getDetalles()) {
            validarDetalle(item);
            ListaPrecioArticulo precio = facturaEscrituraRepository.buscarRequerido(
                    ListaPrecioArticulo.class,
                    item.getListaPrecioArticuloId(),
                    "listaPrecioArticuloId"
            );
            double precioUnitario = item.getPrecioUnitario() == null
                    ? precio.getPrecioVenta()
                    : item.getPrecioUnitario();
            double subtotal = item.getCantidad() * precioUnitario;

            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(precio);
            detalle.setDescripcion(
                    item.getDescripcion() == null || item.getDescripcion().isBlank()
                            ? precio.getArticulo().getDenominacion()
                            : item.getDescripcion().trim()
            );
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setImporteNeto(subtotal);
            detalle.setImporteIva(0);
            detalle.setImporteSubtotal(subtotal);
            factura.agregarDetalle(detalle);
            importeTotal += subtotal;
        }

        double importeCobrado = solicitud.getImporteCobrado() == null
                ? 0
                : solicitud.getImporteCobrado();
        if (importeCobrado < 0 || importeCobrado > importeTotal) {
            throw new IllegalArgumentException("importeCobrado debe estar entre 0 y el importe total");
        }
        factura.setImporteTotal(importeTotal);
        factura.setImporteCobrado(importeCobrado);
        factura.setImporteSaldo(importeTotal - importeCobrado);

        facturaEscrituraRepository.guardar(factura);
        return new FacturaCreadaDTO(
                factura.getId(),
                factura.getNumero(),
                factura.getEstado(),
                factura.getImporteTotal(),
                factura.getDetalles().size()
        );
    }

    private void validarSolicitud(FacturaCreacionDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }
        if (solicitud.getNumero() == null || solicitud.getNumero() <= 0) {
            throw new IllegalArgumentException("numero debe ser mayor que cero");
        }
        if (solicitud.getFechaEmision() == null) {
            throw new IllegalArgumentException("fechaEmision es obligatoria");
        }
        if (solicitud.getEstado() == null || solicitud.getEstado().isBlank()) {
            throw new IllegalArgumentException("estado es obligatorio");
        }
        if (solicitud.getDetalles() == null || solicitud.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La factura debe tener al menos un detalle");
        }
    }

    private void validarDetalle(FacturaDetalleCreacionDTO item) {
        if (item == null) {
            throw new IllegalArgumentException("Los detalles no pueden contener valores nulos");
        }
        if (item.getCantidad() == null || item.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad de cada detalle debe ser mayor que cero");
        }
        if (item.getPrecioUnitario() != null && item.getPrecioUnitario() < 0) {
            throw new IllegalArgumentException("precioUnitario no puede ser negativo");
        }
    }
}
