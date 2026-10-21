package com.example.demo.service;

import com.example.demo.dto.FacturaDetalleRequestDTO;
import com.example.demo.dto.FacturaDetalleVistaDTO;
import com.example.demo.dto.FacturaReporteDTO;
import com.example.demo.dto.FacturaRequestDTO;
import com.example.demo.dto.FacturaVistaDTO;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.model.Articulo;
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

    private static final double IVA_PREDETERMINADO = 21.0;

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
                fechaDesde, fechaHasta, estado, montoMinimo
        );
    }

    @Transactional(readOnly = true)
    public FacturaVistaDTO obtenerPorId(Long id) {
        FacturaVenta factura = facturaEscrituraRepository.buscarFactura(id);
        Cliente cliente = factura.getCliente();
        Usuario usuario = factura.getUsuarioCarga();

        List<FacturaDetalleVistaDTO> detalles = factura.getDetalles().stream()
                .map(detalle -> {
                    ListaPrecioArticulo precio = detalle.getListaPrecioArticulo();
                    Articulo articulo = precio.getArticulo();
                    double porcentajeIva = detalle.getPorcentajeIva();
                    if (porcentajeIva == 0 && detalle.getImporteNeto() != 0
                            && detalle.getImporteIva() != 0) {
                        porcentajeIva = redondear(
                                detalle.getImporteIva() * 100 / detalle.getImporteNeto()
                        );
                    }
                    return new FacturaDetalleVistaDTO(
                            detalle.getId(),
                            articulo.getId(),
                            articulo.getCodigo(),
                            articulo.getDenominacion(),
                            precio.getId(),
                            precio.getListaPrecio().getDenominacion(),
                            detalle.getDescripcion(),
                            detalle.getCantidad(),
                            detalle.getPrecioUnitario(),
                            detalle.getPorcentajeBonificacion(),
                            porcentajeIva,
                            detalle.getImporteNeto(),
                            detalle.getImporteIva(),
                            detalle.getImporteSubtotal()
                    );
                })
                .toList();

        return new FacturaVistaDTO(
                factura.getId(),
                factura.getNumero(),
                factura.getFechaEmision(),
                factura.getPuntoVenta().getId(),
                factura.getPuntoVenta().getDescripcion(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? "Consumidor Final" : cliente.getDenominacion(),
                factura.getCondicionIva().getId(),
                factura.getCondicionIva().getDenominacion(),
                factura.getTipoMoneda().getId(),
                factura.getTipoMoneda().getDenominacion(),
                usuario.getId(),
                usuario.getUsuario(),
                factura.getEstado(),
                factura.getImporteCobrado(),
                factura.getImporteSaldo(),
                factura.getImporteTotal(),
                factura.getObservaciones(),
                factura.getFechaAnulacion(),
                detalles
        );
    }

    // CONSIGNA ESCRITURA - 2.a y 2.b: la creacion completa es atomica.
    // Si falla una relacion o un detalle, @Transactional revierte toda la operacion.
    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO solicitud) {
        validarCreacion(solicitud);
        if (facturaEscrituraRepository.existeNumero(solicitud.getNumero())) {
            throw new IllegalArgumentException(
                    "Ya existe una factura con el numero " + solicitud.getNumero()
            );
        }

        Usuario usuario = buscarUsuario(solicitud.getUsuarioId());
        FacturaVenta factura = new FacturaVenta();
        Date ahora = new Date();
        factura.setFechaAlta(ahora);
        factura.setUsuarioCarga(usuario);
        factura.setFechaModificacion(ahora);
        factura.setUsuarioModificacion(usuario);

        // CONSIGNA ESCRITURA - 2.b: mapeo Request DTO -> entidad y busqueda de relaciones por ID.
        aplicarCabecera(factura, solicitud, true);
        reemplazarDetalles(factura, solicitud.getDetalles());
        aplicarCobro(factura, solicitud.getImporteCobrado());

        // CONSIGNA ESCRITURA - 2.b: persistir FacturaVenta guarda los detalles
        // por cascade = ALL configurado en FacturaVenta.detalles.
        facturaEscrituraRepository.guardar(factura);
        return obtenerReporte(factura.getId());
    }

    // CONSIGNA ESCRITURA - 2.a y 2.c: actualiza cabecera y, si se reciben,
    // reemplaza todos los items dentro de una unica transaccion.
    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }

        FacturaVenta factura = facturaEscrituraRepository.buscarFactura(id);
        if (solicitud.getNumero() != null
                && facturaEscrituraRepository.existeNumeroEnOtraFactura(solicitud.getNumero(), id)) {
            throw new IllegalArgumentException(
                    "Ya existe otra factura con el numero " + solicitud.getNumero()
            );
        }

        boolean actualizacionCompleta = solicitud.getNumero() != null
                || solicitud.getFechaEmision() != null
                || solicitud.getPuntoVentaId() != null
                || solicitud.getCondicionIvaId() != null
                || solicitud.getTipoMonedaId() != null
                || solicitud.getDetalles() != null;
        aplicarCabecera(factura, solicitud, actualizacionCompleta);
        if (solicitud.getDetalles() != null) {
            if (solicitud.getDetalles().isEmpty()) {
                throw new IllegalArgumentException("La factura debe tener al menos un detalle");
            }
            reemplazarDetalles(factura, solicitud.getDetalles());
        }

        Double cobrado = solicitud.getImporteCobrado() != null
                ? solicitud.getImporteCobrado()
                : factura.getImporteCobrado();
        aplicarCobro(factura, cobrado);

        Date ahora = new Date();
        factura.setFechaModificacion(ahora);
        if (solicitud.getUsuarioId() != null) {
            factura.setUsuarioModificacion(buscarUsuario(solicitud.getUsuarioId()));
        }
        actualizarFechaAnulacion(factura, ahora);

        facturaEscrituraRepository.guardar(factura);
        return obtenerReporte(factura.getId());
    }

    // CONSIGNA ESCRITURA - 2.a, 2.d y 3.1: eliminacion fisica.
    // Los detalles se eliminan en cascada por orphanRemoval/cascade ALL.
    @Transactional
    public void eliminar(Long id) {
        FacturaVenta factura = facturaEscrituraRepository.buscarFactura(id);
        facturaEscrituraRepository.eliminar(factura);
    }

    private void aplicarCabecera(
            FacturaVenta factura,
            FacturaRequestDTO solicitud,
            boolean creacion
    ) {
        if (solicitud.getNumero() != null) {
            if (solicitud.getNumero() <= 0) {
                throw new IllegalArgumentException("numero debe ser mayor que cero");
            }
            factura.setNumero(solicitud.getNumero());
        }
        if (solicitud.getFechaEmision() != null) {
            factura.setFechaEmision(java.sql.Timestamp.valueOf(solicitud.getFechaEmision()));
        }
        if (solicitud.getPuntoVentaId() != null) {
            factura.setPuntoVenta(facturaEscrituraRepository.buscarRequerido(
                    PuntoVenta.class, solicitud.getPuntoVentaId(), "puntoVentaId"
            ));
        }
        if (solicitud.getCondicionIvaId() != null) {
            factura.setCondicionIva(facturaEscrituraRepository.buscarRequerido(
                    CondicionIva.class, solicitud.getCondicionIvaId(), "condicionIvaId"
            ));
        }
        if (solicitud.getTipoMonedaId() != null) {
            factura.setTipoMoneda(facturaEscrituraRepository.buscarRequerido(
                    TipoMoneda.class, solicitud.getTipoMonedaId(), "tipoMonedaId"
            ));
        }
        if (creacion || solicitud.getClienteId() != null) {
            Cliente cliente = solicitud.getClienteId() == null
                    ? null
                    : facturaEscrituraRepository.buscarRequerido(
                            Cliente.class, solicitud.getClienteId(), "clienteId"
                    );
            factura.setCliente(cliente);
            if (cliente != null && cliente.getCondicionIva() != null) {
                if (solicitud.getCondicionIvaId() != null
                        && !cliente.getCondicionIva().getId().equals(solicitud.getCondicionIvaId())) {
                    throw new IllegalArgumentException(
                            "condicionIvaId no corresponde a la condicion de IVA del cliente"
                    );
                }
                factura.setCondicionIva(cliente.getCondicionIva());
            }
        }
        if (solicitud.getEstado() != null) {
            if (solicitud.getEstado().isBlank()) {
                throw new IllegalArgumentException("estado no puede estar vacio");
            }
            factura.setEstado(solicitud.getEstado().trim().toUpperCase());
        }
        if (solicitud.getObservaciones() != null) {
            factura.setObservaciones(solicitud.getObservaciones().trim());
        }
    }

    // CONSIGNA ESCRITURA - 2.b y 2.c: calcula neto, bonificacion, IVA,
    // subtotal y total sin aceptar importes calculados por el cliente.
    private void reemplazarDetalles(
            FacturaVenta factura,
            List<FacturaDetalleRequestDTO> solicitudes
    ) {
        factura.getDetalles().clear();
        double total = 0;

        for (FacturaDetalleRequestDTO solicitud : solicitudes) {
            validarDetalle(solicitud);
            Articulo articulo = facturaEscrituraRepository.buscarRequerido(
                    Articulo.class, solicitud.getArticuloId(), "articuloId"
            );
            ListaPrecioArticulo precio = facturaEscrituraRepository.buscarRequerido(
                    ListaPrecioArticulo.class,
                    solicitud.getListaPrecioArticuloId(),
                    "listaPrecioArticuloId"
            );
            if (!precio.getArticulo().getId().equals(articulo.getId())) {
                throw new IllegalArgumentException(
                        "listaPrecioArticuloId no corresponde al articuloId indicado"
                );
            }

            double precioUnitario = solicitud.getPrecioUnitario() == null
                    ? precio.getPrecioVenta()
                    : solicitud.getPrecioUnitario();
            double bonificacion = valorOZero(solicitud.getPorcentajeBonificacion());
            double porcentajeIva = solicitud.getPorcentajeIva() == null
                    ? IVA_PREDETERMINADO
                    : solicitud.getPorcentajeIva();

            double bruto = solicitud.getCantidad() * precioUnitario;
            double importeNeto = redondear(bruto - (bruto * bonificacion / 100));
            double importeIva = redondear(importeNeto * porcentajeIva / 100);
            double subtotal = redondear(importeNeto + importeIva);

            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(precio);
            detalle.setDescripcion(
                    solicitud.getDescripcion() == null || solicitud.getDescripcion().isBlank()
                            ? articulo.getDenominacion()
                            : solicitud.getDescripcion().trim()
            );
            detalle.setCantidad(solicitud.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setPorcentajeBonificacion(bonificacion);
            detalle.setPorcentajeIva(porcentajeIva);
            detalle.setImporteNeto(importeNeto);
            detalle.setImporteIva(importeIva);
            detalle.setImporteSubtotal(subtotal);
            factura.agregarDetalle(detalle);
            total += subtotal;
        }
        factura.setImporteTotal(redondear(total));
    }

    private void aplicarCobro(FacturaVenta factura, Double importeCobrado) {
        double cobrado = valorOZero(importeCobrado);
        if (cobrado < 0 || cobrado > factura.getImporteTotal()) {
            throw new IllegalArgumentException(
                    "importeCobrado debe estar entre 0 y el importe total"
            );
        }
        factura.setImporteCobrado(redondear(cobrado));
        factura.setImporteSaldo(redondear(factura.getImporteTotal() - cobrado));
    }

    private void validarCreacion(FacturaRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }
        if (solicitud.getNumero() == null || solicitud.getFechaEmision() == null
                || solicitud.getPuntoVentaId() == null || solicitud.getCondicionIvaId() == null
                || solicitud.getTipoMonedaId() == null || solicitud.getUsuarioId() == null) {
            throw new IllegalArgumentException(
                    "numero, fechaEmision, puntoVentaId, condicionIvaId, " +
                    "tipoMonedaId y usuarioId son obligatorios"
            );
        }
        if (solicitud.getEstado() == null || solicitud.getEstado().isBlank()) {
            throw new IllegalArgumentException("estado es obligatorio");
        }
        if (solicitud.getDetalles() == null || solicitud.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La factura debe tener al menos un detalle");
        }
    }

    private void validarDetalle(FacturaDetalleRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Los detalles no pueden contener valores nulos");
        }
        if (solicitud.getArticuloId() == null || solicitud.getListaPrecioArticuloId() == null) {
            throw new IllegalArgumentException(
                    "articuloId y listaPrecioArticuloId son obligatorios en cada detalle"
            );
        }
        if (solicitud.getCantidad() == null || solicitud.getCantidad() <= 0) {
            throw new IllegalArgumentException("cantidad debe ser mayor que cero");
        }
        if (solicitud.getPrecioUnitario() != null && solicitud.getPrecioUnitario() < 0) {
            throw new IllegalArgumentException("precioUnitario no puede ser negativo");
        }
        validarPorcentaje(solicitud.getPorcentajeBonificacion(), "porcentajeBonificacion", 100);
        validarPorcentaje(solicitud.getPorcentajeIva(), "porcentajeIva", null);
    }

    private void validarPorcentaje(Double valor, String campo, Integer maximo) {
        if (valor != null && (valor < 0 || (maximo != null && valor > maximo))) {
            throw new IllegalArgumentException(campo + " tiene un valor invalido");
        }
    }

    private void actualizarFechaAnulacion(FacturaVenta factura, Date ahora) {
        if ("ANULADA".equalsIgnoreCase(factura.getEstado())) {
            if (factura.getFechaAnulacion() == null) {
                factura.setFechaAnulacion(ahora);
            }
        } else {
            factura.setFechaAnulacion(null);
        }
    }

    private Usuario buscarUsuario(Long id) {
        return facturaEscrituraRepository.buscarRequerido(Usuario.class, id, "usuarioId");
    }

    private FacturaReporteDTO obtenerReporte(Long id) {
        return facturaReporteRepository.buscarReportePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se pudo generar el reporte de la factura " + id
                ));
    }

    private double valorOZero(Double valor) {
        return valor == null ? 0 : valor;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
