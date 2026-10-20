package com.utn.desarrollosoft.contable.servicios;

import com.utn.desarrollosoft.contable.entidades.dto.FacturaVentaDTO;
import com.utn.desarrollosoft.contable.entidades.dto.FacturaVentaDetalleDTO;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class FacturaVentaService {

    private final List<FacturaVentaDTO> facturas = new ArrayList<>();
    private final AtomicLong siguienteNumeroFactura = new AtomicLong(1004);
    private final AtomicLong siguienteDetalleId = new AtomicLong(1);

    public FacturaVentaService() {
        inicializarDatos();
    }

    private void inicializarDatos() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            facturas.add(crearFactura(1001L, sdf.parse("2026-03-15"), "Consumidor Final",
                    "IVA Responsable Inscripto", "Sucursal Central",
                    List.of(
                            new FacturaVentaDetalleDTO(siguienteDetalleId.getAndIncrement(), "Producto A", 2, 500.0),
                            new FacturaVentaDetalleDTO(siguienteDetalleId.getAndIncrement(), "Producto B", 1, 500.0)
                    )));

            facturas.add(crearFactura(1002L, sdf.parse("2026-03-16"), "Juan Pérez",
                    "IVA Responsable Inscripto", "Sucursal Central",
                    List.of(
                            new FacturaVentaDetalleDTO(siguienteDetalleId.getAndIncrement(), "Producto C", 3, 750.0),
                            new FacturaVentaDetalleDTO(siguienteDetalleId.getAndIncrement(), "Producto D", 1, 1000.5)
                    )));

            facturas.add(crearFactura(1003L, sdf.parse("2026-03-17"), "María González",
                    "Monotributo", "Sucursal Norte",
                    List.of(
                            new FacturaVentaDetalleDTO(siguienteDetalleId.getAndIncrement(), "Producto E", 5, 1000.0),
                            new FacturaVentaDetalleDTO(siguienteDetalleId.getAndIncrement(), "Producto F", 1, 3750.0)
                    )));
        } catch (ParseException e) {
            throw new RuntimeException("Error al inicializar facturas de prueba", e);
        }
    }

    private FacturaVentaDTO crearFactura(Long numeroFactura, Date fechaEmision, String clienteDenominacion,
                                         String condicionIva, String puntoVentaDescripcion,
                                         List<FacturaVentaDetalleDTO> detalles) {
        double importeTotal = detalles.stream()
                .mapToDouble(d -> d.getSubtotal() != null ? d.getSubtotal() : 0.0)
                .sum();
        return new FacturaVentaDTO(numeroFactura, fechaEmision, clienteDenominacion,
                condicionIva, puntoVentaDescripcion, importeTotal, detalles);
    }

    public List<FacturaVentaDTO> listarTodas() {
        return new ArrayList<>(facturas);
    }

    public List<FacturaVentaDTO> filtrar(Long numeroFactura, String clienteDenominacion,
                                          String condicionIva, Date fechaDesde, Date fechaHasta) {
        return facturas.stream()
                .filter(f -> numeroFactura == null || f.getNumeroFactura().equals(numeroFactura))
                .filter(f -> clienteDenominacion == null ||
                        f.getClienteDenominacion().toLowerCase().contains(clienteDenominacion.toLowerCase()))
                .filter(f -> condicionIva == null || f.getCondicionIva().equalsIgnoreCase(condicionIva))
                .filter(f -> fechaDesde == null || !f.getFechaEmision().before(fechaDesde))
                .filter(f -> fechaHasta == null || !f.getFechaEmision().after(fechaHasta))
                .collect(Collectors.toList());
    }

    public Optional<FacturaVentaDTO> buscarPorNumero(Long numeroFactura) {
        return facturas.stream()
                .filter(f -> f.getNumeroFactura().equals(numeroFactura))
                .findFirst();
    }

    public FacturaVentaDTO guardar(FacturaVentaDTO factura) {
        recalcularTotales(factura);
        if (factura.getNumeroFactura() == null) {
            factura.setNumeroFactura(siguienteNumeroFactura.getAndIncrement());
        }
        asignarIdsDetalles(factura);
        facturas.add(factura);
        return factura;
    }

    public Optional<FacturaVentaDTO> actualizar(Long numeroFactura, FacturaVentaDTO facturaActualizada) {
        Optional<FacturaVentaDTO> existente = buscarPorNumero(numeroFactura);
        if (existente.isEmpty()) {
            return Optional.empty();
        }
        facturas.remove(existente.get());
        facturaActualizada.setNumeroFactura(numeroFactura);
        recalcularTotales(facturaActualizada);
        asignarIdsDetalles(facturaActualizada);
        facturas.add(facturaActualizada);
        return Optional.of(facturaActualizada);
    }

    public boolean eliminar(Long numeroFactura) {
        return facturas.removeIf(f -> f.getNumeroFactura().equals(numeroFactura));
    }

    private void recalcularTotales(FacturaVentaDTO factura) {
        if (factura.getDetalles() == null) {
            factura.setDetalles(new ArrayList<>());
        }
        for (FacturaVentaDetalleDTO detalle : factura.getDetalles()) {
            detalle.setSubtotal(detalle.getCantidad() * detalle.getPrecioUnitario());
        }
        double total = factura.getDetalles().stream()
                .mapToDouble(d -> d.getSubtotal() != null ? d.getSubtotal() : 0.0)
                .sum();
        factura.setImporteTotal(total);
    }

    private void asignarIdsDetalles(FacturaVentaDTO factura) {
        for (FacturaVentaDetalleDTO detalle : factura.getDetalles()) {
            if (detalle.getId() == null) {
                detalle.setId(siguienteDetalleId.getAndIncrement());
            }
        }
    }
}
