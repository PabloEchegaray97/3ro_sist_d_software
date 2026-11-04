package com.example.demo.service;

import com.example.demo.dto.FacturaDetalleRequestDTO;
import com.example.demo.dto.FacturaDetalleVistaDTO;
import com.example.demo.dto.FacturaReporteDTO;
import com.example.demo.dto.FacturaRequestDTO;
import com.example.demo.dto.FacturaVistaDTO;
import com.example.demo.dto.ArcaEmisionDTO;
import com.example.demo.dto.TotalMonedaDTO;
import com.example.demo.exception.ConflictoNegocioException;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.exception.PrecisionMonetariaException;
import com.example.demo.model.Articulo;
import com.example.demo.model.Cliente;
import com.example.demo.model.CondicionIva;
import com.example.demo.model.FacturaVenta;
import com.example.demo.model.FacturaVentaDetalle;
import com.example.demo.model.EstadoFactura;
import com.example.demo.model.ListaPrecioArticulo;
import com.example.demo.model.ListaPrecio;
import com.example.demo.model.PuntoVenta;
import com.example.demo.model.TipoMoneda;
import com.example.demo.model.Usuario;
import com.example.demo.repository.FacturaEscrituraRepository;
import com.example.demo.repository.FacturaReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private static final BigDecimal IVA_PREDETERMINADO = new BigDecimal("21.00");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final FacturaReporteRepository facturaReporteRepository;
    private final FacturaEscrituraRepository facturaEscrituraRepository;
    private final ArcaHomologacionService arcaHomologacionService;

    @Transactional(readOnly = true)
    // CONSIGNA - PARTE 3.2: el servicio devuelve List<FacturaReporteDTO>
    // y delega en la consulta JPQL dinamica del repositorio.
    public List<FacturaReporteDTO> buscarFacturasFiltradas(
            Date fechaDesde,
            Date fechaHasta,
            String estado,
            BigDecimal montoMinimo
    ) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.after(fechaHasta)) {
            throw new IllegalArgumentException("fechaDesde no puede ser posterior a fechaHasta");
        }
        if (montoMinimo != null && montoMinimo.signum() < 0) {
            throw new IllegalArgumentException("montoMinimo no puede ser negativo");
        }
        return facturaReporteRepository.buscarFacturasFiltradas(
                fechaDesde, fechaHasta, estado, montoMinimo
        );
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> totalesEmitidosPorMoneda() {
        return facturaReporteRepository.totalesEmitidosPorMoneda().stream()
                .filter(total -> total.codigoMoneda() != null && total.total() != null)
                .collect(Collectors.toMap(
                        TotalMonedaDTO::codigoMoneda,
                        TotalMonedaDTO::total,
                        BigDecimal::add,
                        LinkedHashMap::new
                ));
    }

    @Transactional(readOnly = true)
    public FacturaVistaDTO obtenerPorId(Long id) {
        FacturaVenta factura = facturaEscrituraRepository.buscarFactura(id);
        Cliente cliente = factura.getCliente();
        Usuario usuario = factura.getUsuarioCarga();
        ListaPrecio listaPrecio = factura.getListaPrecio();
        if (listaPrecio == null && !factura.getDetalles().isEmpty()) {
            listaPrecio = factura.getDetalles().get(0).getListaPrecioArticulo().getListaPrecio();
        }

        List<FacturaDetalleVistaDTO> detalles = factura.getDetalles().stream()
                .map(detalle -> {
                    ListaPrecioArticulo precio = detalle.getListaPrecioArticulo();
                    Articulo articulo = precio.getArticulo();
                    BigDecimal porcentajeIva = detalle.getPorcentajeIva();
                    if (porcentajeIva.signum() == 0 && detalle.getImporteNeto().signum() != 0
                            && detalle.getImporteIva().signum() != 0) {
                        porcentajeIva = redondear(
                                detalle.getImporteIva().multiply(CIEN)
                                        .divide(detalle.getImporteNeto(), 4, RoundingMode.HALF_UP),
                                4
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
                factura.getPuntoVenta().getNumero(),
                factura.getPuntoVenta().getDescripcion(),
                factura.getPuntoVenta().getDomicilioComercial(),
                factura.getTipoComprobante(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getTipoDocumento().name(),
                cliente == null ? "Consumidor Final" : cliente.getDenominacion(),
                cliente == null ? null : cliente.getCuitCuil(),
                cliente == null ? null : primerValor(cliente.getContacto().getTelefono(), cliente.getContacto().getCelular()),
                cliente == null ? null : cliente.getContacto().getEmail(),
                cliente == null ? null : domicilio(cliente),
                listaPrecio == null ? null : listaPrecio.getId(),
                listaPrecio == null ? null : listaPrecio.getCodigo(),
                listaPrecio == null ? null : listaPrecio.getDenominacion(),
                factura.getCondicionIva().getId(),
                factura.getCondicionIva().getDenominacion(),
                factura.getTipoMoneda().getId(),
                factura.getTipoMoneda().getCodigoAfip(),
                factura.getTipoMoneda().getDenominacion(),
                usuario.getId(),
                usuario.getUsuario(),
                factura.getEstado().name(),
                factura.getImporteCobrado(),
                factura.getImporteSaldo(),
                factura.getImporteTotal(),
                factura.getObservaciones(),
                factura.getFechaAnulacion(),
                factura.getCae(),
                factura.getCaeFechaVencimiento(),
                factura.getResultadoAfip(),
                factura.getMotivoRechazo(),
                detalles
        );
    }

    private String primerValor(String principal, String alternativo) {
        return principal != null && !principal.isBlank() ? principal : alternativo;
    }

    private String domicilio(Cliente cliente) {
        String calle = cliente.getDomicilio().getNombreCalle();
        String numero = cliente.getDomicilio().getNumeroCalle();
        return ((calle == null ? "" : calle.trim()) + " " + (numero == null ? "" : numero.trim())).trim();
    }

    // CONSIGNA ESCRITURA - 2.a y 2.b: la creacion completa es atomica.
    // Si falla una relacion o un detalle, @Transactional revierte toda la operacion.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FacturaReporteDTO crear(FacturaRequestDTO solicitud) {
        validarCreacion(solicitud);
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
        factura.setNumero(null);
        factura.setFechaEmision(null);
        factura.setEstado(EstadoFactura.SIN_EMITIR);
        factura.setCae(null);
        factura.setCaeFechaVencimiento(null);
        factura.setResultadoAfip(null);
        factura.setMotivoRechazo(null);

        // CONSIGNA ESCRITURA - 2.b: persistir FacturaVenta guarda los detalles
        // por cascade = ALL configurado en FacturaVenta.detalles.
        facturaEscrituraRepository.guardar(factura);
        return obtenerReporte(factura.getId());
    }

    // CONSIGNA ESCRITURA - 2.a y 2.c: actualiza cabecera y, si se reciben,
    // reemplaza todos los items dentro de una unica transaccion.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }

        FacturaVenta factura = facturaEscrituraRepository.buscarFacturaParaActualizar(id);
        if (factura.getEstado() != EstadoFactura.SIN_EMITIR) {
            throw new ConflictoNegocioException("Una factura emitida o anulada es inmutable");
        }

        boolean actualizacionCompleta = solicitud.getPuntoVentaId() != null
                || solicitud.getListaPrecioId() != null
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

        BigDecimal cobrado = solicitud.getImporteCobrado() != null
                ? solicitud.getImporteCobrado()
                : factura.getImporteCobrado();
        aplicarCobro(factura, cobrado);

        Date ahora = new Date();
        factura.setFechaModificacion(ahora);
        if (solicitud.getUsuarioId() != null) {
            factura.setUsuarioModificacion(buscarUsuario(solicitud.getUsuarioId()));
        }
        facturaEscrituraRepository.guardar(factura);
        return obtenerReporte(factura.getId());
    }

    // CONSIGNA ESCRITURA - 2.a, 2.d y 3.1: eliminacion fisica.
    // Los detalles se eliminan en cascada por orphanRemoval/cascade ALL.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void eliminar(Long id) {
        FacturaVenta factura = facturaEscrituraRepository.buscarFacturaParaActualizar(id);
        if (factura.getEstado() != EstadoFactura.SIN_EMITIR
                && factura.getEstado() != EstadoFactura.ANULADA) {
            throw new ConflictoNegocioException(
                    "Solo se pueden eliminar definitivamente facturas sin emitir o anuladas"
            );
        }
        facturaEscrituraRepository.eliminar(factura);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ArcaEmisionDTO emitirConArca(Long id) {
        FacturaVenta factura = facturaEscrituraRepository.buscarFacturaParaActualizar(id);
        facturaEscrituraRepository.bloquearPuntoVenta(factura.getPuntoVenta().getId());
        if (factura.getEstado() == EstadoFactura.EMITIDA && factura.getCae() != null) {
            return new ArcaEmisionDTO(
                    true, factura.getId(), factura.getNumero(), factura.getEstado().name(),
                    factura.getCae(), factura.getCaeFechaVencimiento(),
                    "La factura ya fue autorizada por ARCA", List.of()
            );
        }
        if (factura.getEstado() != EstadoFactura.SIN_EMITIR) {
            throw new ConflictoNegocioException("La factura no se encuentra disponible para emitir");
        }

        try {
            Date fechaComprobante = new Date();
            ArcaHomologacionService.ResultadoArca resultado = arcaHomologacionService.emitir(factura, fechaComprobante);
            factura.setResultadoAfip(resultado.resultado());
            factura.setMotivoRechazo(resultado.aprobada() ? null : resultado.mensaje());
            if (resultado.aprobada()) {
                factura.setNumero(resultado.numero());
                factura.setTipoComprobante(11);
                factura.setFechaEmision(fechaComprobante);
                factura.setCae(resultado.cae());
                factura.setCaeFechaVencimiento(resultado.vencimiento());
                factura.setEstado(EstadoFactura.EMITIDA);
            }
            factura.setFechaModificacion(new Date());
            facturaEscrituraRepository.guardar(factura);
            return new ArcaEmisionDTO(
                    resultado.aprobada(), factura.getId(), factura.getNumero(), factura.getEstado().name(),
                    factura.getCae(), factura.getCaeFechaVencimiento(), resultado.mensaje(), resultado.mensajes()
            );
        } catch (ArcaHomologacionService.ArcaException ex) {
            String mensaje = ex.getMessage() == null ? "No se pudo conectar con ARCA" : ex.getMessage();
            factura.setResultadoAfip("ERROR");
            factura.setMotivoRechazo(mensaje);
            factura.setFechaModificacion(new Date());
            facturaEscrituraRepository.guardar(factura);
            return errorEmision(factura, mensaje);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FacturaReporteDTO anular(Long id) {
        FacturaVenta factura = facturaEscrituraRepository.buscarFacturaParaActualizar(id);
        if (factura.getEstado() != EstadoFactura.EMITIDA) {
            throw new ConflictoNegocioException("Solo se pueden anular facturas emitidas");
        }
        Date ahora = new Date();
        factura.setEstado(EstadoFactura.ANULADA);
        factura.setFechaAnulacion(ahora);
        factura.setFechaModificacion(ahora);
        facturaEscrituraRepository.guardar(factura);
        return obtenerReporte(id);
    }

    private ArcaEmisionDTO errorEmision(FacturaVenta factura, String mensaje) {
        return new ArcaEmisionDTO(
                false, factura.getId(), factura.getNumero(), factura.getEstado().name(),
                factura.getCae(), factura.getCaeFechaVencimiento(), mensaje,
                List.of(new ArcaEmisionDTO.MensajeDTO("ERROR", "", mensaje))
        );
    }

    private void aplicarCabecera(
            FacturaVenta factura,
            FacturaRequestDTO solicitud,
            boolean creacion
    ) {
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
        if (solicitud.getListaPrecioId() != null) {
            factura.setListaPrecio(facturaEscrituraRepository.buscarRequerido(
                    ListaPrecio.class, solicitud.getListaPrecioId(), "listaPrecioId"
            ));
        } else if (creacion && factura.getCliente() != null
                && factura.getCliente().getListaPrecioPredeterminada() != null) {
            factura.setListaPrecio(factura.getCliente().getListaPrecioPredeterminada());
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
        BigDecimal total = BigDecimal.ZERO;

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
            if (factura.getListaPrecio() != null
                    && !precio.getListaPrecio().getId().equals(factura.getListaPrecio().getId())) {
                throw new IllegalArgumentException(
                        "listaPrecioArticuloId no pertenece a la lista de precios de la factura"
                );
            }

            // Descripcion, precio e IVA son datos maestros: el cliente HTTP no puede alterarlos.
            BigDecimal precioUnitario = moneda(precio.getPrecioVenta(), "precioUnitario");
            BigDecimal bonificacion = valorOZero(solicitud.getPorcentajeBonificacion());
            BigDecimal porcentajeIva = IVA_PREDETERMINADO;

            BigDecimal bruto = solicitud.getCantidad().multiply(precioUnitario);
            BigDecimal importeNeto = redondear(bruto.subtract(
                    bruto.multiply(bonificacion).divide(CIEN, 8, RoundingMode.HALF_UP)
            ), 2);
            BigDecimal importeIva = redondear(
                    importeNeto.multiply(porcentajeIva).divide(CIEN, 8, RoundingMode.HALF_UP), 2
            );
            BigDecimal subtotal = redondear(importeNeto.add(importeIva), 2);

            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(precio);
            detalle.setDescripcion(articulo.getDenominacion());
            detalle.setCantidad(solicitud.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setPorcentajeBonificacion(bonificacion);
            detalle.setPorcentajeIva(porcentajeIva);
            detalle.setImporteNeto(importeNeto);
            detalle.setImporteIva(importeIva);
            detalle.setImporteSubtotal(subtotal);
            factura.agregarDetalle(detalle);
            total = total.add(subtotal);
        }
        factura.setImporteTotal(redondear(total, 2));
    }

    private void aplicarCobro(FacturaVenta factura, BigDecimal importeCobrado) {
        BigDecimal cobrado = moneda(valorOZero(importeCobrado), "importeCobrado");
        if (cobrado.signum() < 0 || cobrado.compareTo(factura.getImporteTotal()) > 0) {
            throw new IllegalArgumentException(
                    "importeCobrado debe estar entre 0 y el importe total"
            );
        }
        factura.setImporteCobrado(redondear(cobrado, 2));
        factura.setImporteSaldo(redondear(factura.getImporteTotal().subtract(cobrado), 2));
    }

    private void validarCreacion(FacturaRequestDTO solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }
        if (solicitud.getPuntoVentaId() == null || solicitud.getCondicionIvaId() == null
                || solicitud.getListaPrecioId() == null
                || solicitud.getTipoMonedaId() == null || solicitud.getUsuarioId() == null) {
            throw new IllegalArgumentException(
                    "puntoVentaId, listaPrecioId, condicionIvaId, " +
                    "tipoMonedaId y usuarioId son obligatorios"
            );
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
        if (solicitud.getCantidad() == null || solicitud.getCantidad().signum() <= 0) {
            throw new IllegalArgumentException("cantidad debe ser mayor que cero");
        }
        if (solicitud.getPrecioUnitario() != null && solicitud.getPrecioUnitario().signum() < 0) {
            throw new IllegalArgumentException("precioUnitario no puede ser negativo");
        }
        validarPorcentaje(solicitud.getPorcentajeBonificacion(), "porcentajeBonificacion", 100);
        validarPorcentaje(solicitud.getPorcentajeIva(), "porcentajeIva", null);
    }

    private void validarPorcentaje(BigDecimal valor, String campo, Integer maximo) {
        if (valor != null && (valor.signum() < 0
                || (maximo != null && valor.compareTo(BigDecimal.valueOf(maximo)) > 0))) {
            throw new IllegalArgumentException(campo + " tiene un valor invalido");
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

    private BigDecimal valorOZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private BigDecimal redondear(BigDecimal valor, int escala) {
        return valor.setScale(escala, RoundingMode.HALF_UP);
    }

    private BigDecimal moneda(BigDecimal valor, String campo) {
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new PrecisionMonetariaException(campo + " admite como máximo dos decimales");
        }
    }
}
