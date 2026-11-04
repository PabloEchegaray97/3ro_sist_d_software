package com.example.demo.service;

import com.example.demo.dto.DatosMaestrosDTO;
import com.example.demo.dto.EntidadCreadaDTO;
import com.example.demo.dto.ArticuloDisponibleDTO;
import com.example.demo.dto.ArticuloAltaDTO;
import com.example.demo.dto.ArticuloEdicionDTO;
import com.example.demo.dto.ClienteDisponibleDTO;
import com.example.demo.dto.CatalogoItemDTO;
import com.example.demo.dto.PaginaDTO;
import com.example.demo.dto.FacturaOpcionesDTO;
import com.example.demo.dto.SistemaInfoDTO;
import com.example.demo.model.Articulo;
import com.example.demo.model.AuditoriaApp;
import com.example.demo.model.Cliente;
import com.example.demo.model.CondicionIva;
import com.example.demo.model.Contacto;
import com.example.demo.model.Domicilio;
import com.example.demo.model.ListaPrecio;
import com.example.demo.model.ListaPrecioArticulo;
import com.example.demo.model.Marca;
import com.example.demo.model.PuntoVenta;
import com.example.demo.model.Rubro;
import com.example.demo.model.TipoMoneda;
import com.example.demo.model.TipoDocumento;
import com.example.demo.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.example.demo.exception.PrecisionMonetariaException;

@Service
public class DatosMaestrosService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public int migrarCondicionIvaClientesExistentes() {
        Usuario admin = entityManager.createQuery(
                        "SELECT u FROM Usuario u WHERE LOWER(u.usuario) = 'admin' ORDER BY u.id",
                        Usuario.class
                ).setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el usuario admin para migrar las condiciones de IVA"
                ));

        CondicionIva responsableInscripto = buscarOCrearCondicionIva(
                "%responsable inscripto%",
                1,
                "IVA Responsable Inscripto",
                admin
        );
        CondicionIva consumidorFinal = buscarOCrearCondicionIva(
                "%consumidor final%",
                5,
                "Consumidor Final",
                admin
        );

        List<Cliente> clientesSinCondicion = entityManager.createQuery(
                "SELECT c FROM Cliente c WHERE c.condicionIva IS NULL",
                Cliente.class
        ).getResultList();

        clientesSinCondicion.forEach(cliente -> {
            String denominacion = cliente.getDenominacion() == null
                    ? ""
                    : cliente.getDenominacion().trim().toLowerCase();
            cliente.setCondicionIva(
                    denominacion.contains("consumidor final")
                            ? consumidorFinal
                            : responsableInscripto
            );
        });
        return clientesSinCondicion.size();
    }

    private CondicionIva buscarOCrearCondicionIva(
            String patron,
            int codigoAfip,
            String denominacion,
            Usuario admin
    ) {
        return entityManager.createQuery(
                        "SELECT c FROM CondicionIva c " +
                                "WHERE LOWER(c.denominacion) LIKE :patron ORDER BY c.id",
                        CondicionIva.class
                ).setParameter("patron", patron)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    CondicionIva condicion = auditar(new CondicionIva(), admin);
                    condicion.setCodigoAfip(codigoAfip);
                    condicion.setDenominacion(denominacion);
                    entityManager.persist(condicion);
                    return condicion;
                });
    }

    @Transactional
    public int migrarListaPrecioClientesExistentes() {
        ListaPrecio listaMinorista = buscarListaPrecioMinorista();
        if (listaMinorista == null) {
            return 0;
        }
        List<Cliente> clientesSinLista = entityManager.createQuery(
                "SELECT c FROM Cliente c WHERE c.listaPrecioPredeterminada IS NULL",
                Cliente.class
        ).getResultList();
        clientesSinLista.forEach(cliente -> cliente.setListaPrecioPredeterminada(listaMinorista));
        return clientesSinLista.size();
    }

    private ListaPrecio buscarListaPrecioMinorista() {
        return entityManager.createQuery("""
                SELECT lista
                FROM ListaPrecio lista
                ORDER BY CASE
                    WHEN UPPER(lista.codigo) = 'MINORISTA' THEN 0
                    WHEN LOWER(lista.denominacion) LIKE '%minorista%' THEN 1
                    ELSE 2
                END, lista.id
                """, ListaPrecio.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void prepararCatalogoPreciosInicial() {
        Usuario admin = entityManager.createQuery(
                        "SELECT u FROM Usuario u WHERE LOWER(u.usuario) = 'admin' ORDER BY u.id",
                        Usuario.class
                ).setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el usuario admin para preparar las listas de precios"
                ));

        ListaPrecio minorista = buscarListaPorCodigo("MINORISTA");
        List<ListaPrecio> listasGenerales = entityManager.createQuery("""
                SELECT lista
                FROM ListaPrecio lista
                WHERE UPPER(lista.codigo) IN ('LP-GENERAL', 'GENERAL')
                   OR LOWER(lista.denominacion) = 'lista general'
                ORDER BY lista.id
                """, ListaPrecio.class).getResultList();

        if (minorista == null && !listasGenerales.isEmpty()) {
            minorista = listasGenerales.remove(0);
            minorista.setCodigo("MINORISTA");
            minorista.setDenominacion("Precio minorista");
            actualizarAuditoria(minorista, admin);
        }
        if (minorista == null) {
            minorista = crearListaPrecio("MINORISTA", "Precio minorista", admin);
        }
        for (ListaPrecio listaGeneral : listasGenerales) {
            if (!listaGeneral.getId().equals(minorista.getId())) {
                fusionarListaPrecio(listaGeneral, minorista);
            }
        }

        ListaPrecio mayorista = buscarListaPorCodigo("MAYORISTA");
        if (mayorista == null) {
            mayorista = crearListaPrecio("MAYORISTA", "Precio mayorista", admin);
        }

        Rubro rubro = buscarOCrearRubroCatalogo(admin);
        Marca marca = buscarOCrearMarcaCatalogo(admin);
        List<ArticuloSemilla> articulos = List.of(
                new ArticuloSemilla("ART-001", "Teclado mecánico", new BigDecimal("45000"), new BigDecimal("39000")),
                new ArticuloSemilla("ART-002", "Mouse inalámbrico", new BigDecimal("25000"), new BigDecimal("21500")),
                new ArticuloSemilla("ART-003", "Monitor 24 pulgadas", new BigDecimal("180000"), new BigDecimal("158000")),
                new ArticuloSemilla("ART-004", "Auriculares USB", new BigDecimal("38000"), new BigDecimal("32500")),
                new ArticuloSemilla("ART-005", "Webcam Full HD", new BigDecimal("52000"), new BigDecimal("44500"))
        );

        for (ArticuloSemilla datos : articulos) {
            Articulo articulo = buscarOCrearArticulo(datos, rubro, marca, admin);
            crearPrecioSiFalta(minorista, articulo, datos.precioMinorista(), admin);
            crearPrecioSiFalta(mayorista, articulo, datos.precioMayorista(), admin);
        }
        entityManager.flush();
    }

    private ListaPrecio buscarListaPorCodigo(String codigo) {
        return entityManager.createQuery(
                        "SELECT lista FROM ListaPrecio lista WHERE UPPER(lista.codigo) = :codigo ORDER BY lista.id",
                        ListaPrecio.class
                ).setParameter("codigo", codigo)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private ListaPrecio crearListaPrecio(String codigo, String denominacion, Usuario usuario) {
        ListaPrecio lista = auditar(new ListaPrecio(), usuario);
        lista.setCodigo(codigo);
        lista.setDenominacion(denominacion);
        entityManager.persist(lista);
        return lista;
    }

    private void fusionarListaPrecio(ListaPrecio origen, ListaPrecio destino) {
        List<ListaPrecioArticulo> preciosOrigen = entityManager.createQuery("""
                SELECT precio
                FROM ListaPrecioArticulo precio
                JOIN FETCH precio.articulo
                WHERE precio.listaPrecio = :origen
                """, ListaPrecioArticulo.class)
                .setParameter("origen", origen)
                .getResultList();

        for (ListaPrecioArticulo precioOrigen : preciosOrigen) {
            ListaPrecioArticulo precioDestino = entityManager.createQuery("""
                    SELECT precio
                    FROM ListaPrecioArticulo precio
                    WHERE precio.listaPrecio = :destino
                      AND precio.articulo = :articulo
                    """, ListaPrecioArticulo.class)
                    .setParameter("destino", destino)
                    .setParameter("articulo", precioOrigen.getArticulo())
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            if (precioDestino == null) {
                precioOrigen.setListaPrecio(destino);
            } else {
                entityManager.createQuery("""
                        UPDATE FacturaVentaDetalle detalle
                        SET detalle.listaPrecioArticulo = :destino
                        WHERE detalle.listaPrecioArticulo = :origen
                        """)
                        .setParameter("destino", precioDestino)
                        .setParameter("origen", precioOrigen)
                        .executeUpdate();
                entityManager.remove(precioOrigen);
            }
        }

        entityManager.createQuery("""
                UPDATE FacturaVenta factura
                SET factura.listaPrecio = :destino
                WHERE factura.listaPrecio = :origen
                """)
                .setParameter("destino", destino)
                .setParameter("origen", origen)
                .executeUpdate();
        entityManager.createQuery("""
                UPDATE Cliente cliente
                SET cliente.listaPrecioPredeterminada = :destino
                WHERE cliente.listaPrecioPredeterminada = :origen
                """)
                .setParameter("destino", destino)
                .setParameter("origen", origen)
                .executeUpdate();
        entityManager.flush();
        entityManager.remove(origen);
    }

    private Rubro buscarOCrearRubroCatalogo(Usuario usuario) {
        return entityManager.createQuery(
                        "SELECT rubro FROM Rubro rubro WHERE rubro.codigo = 9001 ORDER BY rubro.id",
                        Rubro.class
                ).setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    Rubro rubro = auditar(new Rubro(), usuario);
                    rubro.setCodigo(9001);
                    rubro.setDenominacion("Tecnología");
                    entityManager.persist(rubro);
                    return rubro;
                });
    }

    private Marca buscarOCrearMarcaCatalogo(Usuario usuario) {
        return entityManager.createQuery(
                        "SELECT marca FROM Marca marca WHERE marca.codigo = 9001 ORDER BY marca.id",
                        Marca.class
                ).setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    Marca marca = auditar(new Marca(), usuario);
                    marca.setCodigo(9001);
                    marca.setDenominacion("Genérica");
                    entityManager.persist(marca);
                    return marca;
                });
    }

    private Articulo buscarOCrearArticulo(
            ArticuloSemilla datos,
            Rubro rubro,
            Marca marca,
            Usuario usuario
    ) {
        return entityManager.createQuery(
                        "SELECT articulo FROM Articulo articulo WHERE UPPER(articulo.codigo) = :codigo ORDER BY articulo.id",
                        Articulo.class
                ).setParameter("codigo", datos.codigo())
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    Articulo articulo = auditar(new Articulo(), usuario);
                    articulo.setCodigo(datos.codigo());
                    articulo.setDenominacion(datos.denominacion());
                    articulo.setRubro(rubro);
                    articulo.setMarca(marca);
                    entityManager.persist(articulo);
                    return articulo;
                });
    }

    private void crearPrecioSiFalta(
            ListaPrecio lista,
            Articulo articulo,
            BigDecimal precioVenta,
            Usuario usuario
    ) {
        boolean existe = entityManager.createQuery("""
                SELECT COUNT(precio)
                FROM ListaPrecioArticulo precio
                WHERE precio.listaPrecio = :lista
                  AND precio.articulo = :articulo
                """, Long.class)
                .setParameter("lista", lista)
                .setParameter("articulo", articulo)
                .getSingleResult() > 0;
        if (!existe) {
            ListaPrecioArticulo precio = auditar(new ListaPrecioArticulo(), usuario);
            precio.setListaPrecio(lista);
            precio.setArticulo(articulo);
            precio.setPrecioVenta(moneda(precioVenta, "precioVenta"));
            entityManager.persist(precio);
        }
    }

    private void actualizarAuditoria(AuditoriaApp entidad, Usuario usuario) {
        entidad.setFechaModificacion(new Date());
        entidad.setUsuarioModificacion(usuario);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ArticuloDisponibleDTO> buscarArticulosDisponibles(
            String busqueda,
            int pagina,
            int tamanio,
            Long listaPrecioId,
            String estado
    ) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.max(1, Math.min(tamanio, 100));
        String patron = patron(busqueda);
        String filtro = " WHERE " + condicionEstado("articulo", estado)
                + " AND " + condicionEstado("precio", estado) + " " + """
                AND (:listaPrecioId IS NULL OR lista.id = :listaPrecioId)
                AND (:patron = '%%' OR
                    LOWER(CAST(precio.id AS string)) LIKE :patron OR
                    LOWER(CAST(articulo.id AS string)) LIKE :patron OR
                    LOWER(articulo.codigo) LIKE :patron OR
                    LOWER(articulo.denominacion) LIKE :patron OR
                    LOWER(CAST(rubro.codigo AS string)) LIKE :patron OR
                    LOWER(rubro.denominacion) LIKE :patron OR
                    LOWER(CAST(marca.codigo AS string)) LIKE :patron OR
                    LOWER(COALESCE(marca.denominacion, '')) LIKE :patron OR
                    LOWER(CAST(lista.id AS string)) LIKE :patron OR
                    LOWER(lista.codigo) LIKE :patron OR
                    LOWER(lista.denominacion) LIKE :patron OR
                    LOWER(CAST(precio.precioVenta AS string)) LIKE :patron)
                """;

        List<ArticuloDisponibleDTO> contenido = entityManager.createQuery("""
                SELECT new com.example.demo.dto.ArticuloDisponibleDTO(
                    precio.id,
                    articulo.id,
                    articulo.codigo,
                    articulo.denominacion,
                    rubro.codigo,
                    rubro.denominacion,
                    marca.codigo,
                    marca.denominacion,
                    lista.id,
                    lista.codigo,
                    precio.precioVenta,
                    articulo.fechaBaja
                )
                FROM ListaPrecioArticulo precio
                JOIN precio.articulo articulo
                JOIN articulo.rubro rubro
                LEFT JOIN articulo.marca marca
                JOIN precio.listaPrecio lista
                """ + filtro + """
                ORDER BY articulo.denominacion, lista.denominacion
                """, ArticuloDisponibleDTO.class)
                .setParameter("patron", patron)
                .setParameter("listaPrecioId", listaPrecioId)
                .setFirstResult(paginaSegura * tamanioSeguro)
                .setMaxResults(tamanioSeguro)
                .getResultList();

        long total = entityManager.createQuery("""
                SELECT COUNT(precio.id)
                FROM ListaPrecioArticulo precio
                JOIN precio.articulo articulo
                JOIN articulo.rubro rubro
                LEFT JOIN articulo.marca marca
                JOIN precio.listaPrecio lista
                """ + filtro, Long.class)
                .setParameter("patron", patron)
                .setParameter("listaPrecioId", listaPrecioId)
                .getSingleResult();

        return pagina(contenido, paginaSegura, tamanioSeguro, total);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ClienteDisponibleDTO> buscarClientesDisponibles(
            String busqueda,
            int pagina,
            int tamanio,
            String orden,
            String estado
    ) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.max(1, Math.min(tamanio, 100));
        String patron = patron(busqueda);
        String filtro = " WHERE " + condicionEstado("cliente", estado) + " " + """
                AND (:patron = '%%' OR
                    LOWER(CAST(cliente.id AS string)) LIKE :patron OR
                    LOWER(CAST(cliente.tipoDocumento AS string)) LIKE :patron OR
                    LOWER(cliente.cuitCuil) LIKE :patron OR
                    LOWER(cliente.denominacion) LIKE :patron OR
                    LOWER(CAST(condicion.id AS string)) LIKE :patron OR
                    LOWER(COALESCE(condicion.denominacion, '')) LIKE :patron OR
                    LOWER(COALESCE(contacto.email, '')) LIKE :patron OR
                    LOWER(COALESCE(contacto.telefono, '')) LIKE :patron OR
                    LOWER(COALESCE(contacto.celular, '')) LIKE :patron OR
                    LOWER(COALESCE(domicilio.nombreCalle, '')) LIKE :patron OR
                    LOWER(COALESCE(domicilio.numeroCalle, '')) LIKE :patron OR
                    LOWER(CAST(listaCliente.id AS string)) LIKE :patron OR
                    LOWER(COALESCE(listaCliente.codigo, '')) LIKE :patron OR
                    LOWER(COALESCE(listaCliente.denominacion, '')) LIKE :patron OR
                    LOWER(CAST(cliente.fechaAlta AS string)) LIKE :patron)
                """;

        List<ClienteDisponibleDTO> contenido = entityManager.createQuery("""
                SELECT new com.example.demo.dto.ClienteDisponibleDTO(
                    cliente.id,
                    cliente.tipoDocumento,
                    cliente.cuitCuil,
                    cliente.denominacion,
                    condicion.id,
                    condicion.denominacion,
                    contacto.email,
                    contacto.telefono,
                    contacto.celular,
                    domicilio.nombreCalle,
                    domicilio.numeroCalle,
                    listaCliente.id,
                    listaCliente.codigo,
                    listaCliente.denominacion,
                    cliente.fechaAlta,
                    cliente.fechaBaja,
                    MAX(factura.fechaEmision)
                )
                FROM Cliente cliente
                LEFT JOIN cliente.condicionIva condicion
                JOIN cliente.contacto contacto
                JOIN cliente.domicilio domicilio
                LEFT JOIN cliente.listaPrecioPredeterminada listaCliente
                LEFT JOIN FacturaVenta factura ON factura.cliente = cliente
                """ + filtro + """
                GROUP BY cliente.id, cliente.tipoDocumento, cliente.cuitCuil, cliente.denominacion,
                    condicion.id, condicion.denominacion,
                    contacto.email, contacto.telefono, contacto.celular,
                    domicilio.nombreCalle, domicilio.numeroCalle,
                    listaCliente.id, listaCliente.codigo, listaCliente.denominacion,
                    cliente.fechaAlta, cliente.fechaBaja
                """ + ordenClientes(orden), ClienteDisponibleDTO.class)
                .setParameter("patron", patron)
                .setFirstResult(paginaSegura * tamanioSeguro)
                .setMaxResults(tamanioSeguro)
                .getResultList();

        long total = entityManager.createQuery("""
                SELECT COUNT(cliente.id)
                FROM Cliente cliente
                LEFT JOIN cliente.condicionIva condicion
                JOIN cliente.contacto contacto
                JOIN cliente.domicilio domicilio
                LEFT JOIN cliente.listaPrecioPredeterminada listaCliente
                """ + filtro, Long.class)
                .setParameter("patron", patron)
                .getSingleResult();

        return pagina(contenido, paginaSegura, tamanioSeguro, total);
    }

    private String ordenClientes(String orden) {
        if ("COMPRADOR_RECIENTE".equalsIgnoreCase(orden)) {
            return " ORDER BY MAX(factura.fechaEmision) DESC, cliente.denominacion";
        }
        return " ORDER BY cliente.fechaAlta DESC, cliente.id DESC";
    }

    private String patron(String busqueda) {
        return "%" + (busqueda == null ? "" : busqueda.trim().toLowerCase()) + "%";
    }

    private <T> PaginaDTO<T> pagina(List<T> contenido, int pagina, int tamanio, long total) {
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil((double) total / tamanio);
        return new PaginaDTO<>(contenido, pagina, tamanio, total, totalPaginas);
    }

    @Transactional(readOnly = true)
    public FacturaOpcionesDTO buscarOpcionesFactura() {
        List<FacturaOpcionesDTO.OpcionDTO> puntosVenta = entityManager.createQuery(
                        "SELECT p FROM PuntoVenta p ORDER BY p.numero",
                        PuntoVenta.class
                ).getResultList().stream()
                .map(p -> opcion(p.getId(), p.getNumero() + " - " + p.getDescripcion()))
                .toList();

        List<FacturaOpcionesDTO.ClienteOpcionDTO> clientes = entityManager.createQuery(
                        "SELECT c FROM Cliente c ORDER BY c.denominacion",
                        Cliente.class
                ).getResultList().stream()
                .map(c -> new FacturaOpcionesDTO.ClienteOpcionDTO(
                        c.getId(),
                        c.getDenominacion(),
                        c.getCondicionIva() == null ? null : c.getCondicionIva().getId(),
                        c.getListaPrecioPredeterminada() == null
                                ? null
                                : c.getListaPrecioPredeterminada().getId(),
                        c.getListaPrecioPredeterminada() == null
                                ? null
                                : c.getListaPrecioPredeterminada().getDenominacion()
                ))
                .toList();

        List<FacturaOpcionesDTO.OpcionDTO> condicionesIva = entityManager.createQuery(
                        "SELECT c FROM CondicionIva c ORDER BY c.denominacion",
                        CondicionIva.class
                ).getResultList().stream()
                .map(c -> opcion(c.getId(), c.getDenominacion()))
                .toList();

        List<FacturaOpcionesDTO.OpcionDTO> tiposMoneda = entityManager.createQuery(
                        "SELECT t FROM TipoMoneda t ORDER BY t.denominacion",
                        TipoMoneda.class
                ).getResultList().stream()
                .map(t -> opcion(t.getId(), t.getDenominacion() + " (" + t.getSimbolo() + ")"))
                .toList();

        List<FacturaOpcionesDTO.OpcionDTO> listasPrecio = entityManager.createQuery(
                        "SELECT lista FROM ListaPrecio lista ORDER BY lista.denominacion",
                        ListaPrecio.class
                ).getResultList().stream()
                .map(lista -> opcion(
                        lista.getId(),
                        lista.getDenominacion() + " (" + lista.getCodigo() + ")"
                ))
                .toList();

        FacturaOpcionesDTO.OpcionDTO usuario = entityManager.createQuery(
                        "SELECT u FROM Usuario u WHERE LOWER(u.usuario) = 'admin' ORDER BY u.id",
                        Usuario.class
                ).setMaxResults(1)
                .getResultList().stream()
                .findFirst()
                .map(u -> opcion(u.getId(), u.getNombre() + " " + u.getApellido() + " (admin)"))
                .orElse(null);

        ListaPrecio listaMinorista = buscarListaPrecioMinorista();
        FacturaOpcionesDTO.OpcionDTO listaPrecioConsumidorFinal = listaMinorista == null
                ? null
                : opcion(listaMinorista.getId(), listaMinorista.getDenominacion());

        return new FacturaOpcionesDTO(
                puntosVenta,
                clientes,
                condicionesIva,
                tiposMoneda,
                listasPrecio,
                listaPrecioConsumidorFinal,
                usuario
        );
    }

    @Transactional(readOnly = true)
    public SistemaInfoDTO buscarInfoSistema() {
        List<SistemaInfoDTO.PuntoVentaInfo> puntosVenta = entityManager.createQuery(
                        "SELECT p FROM PuntoVenta p WHERE p.fechaBaja IS NULL ORDER BY p.numero",
                        PuntoVenta.class
                ).getResultList().stream()
                .map(p -> new SistemaInfoDTO.PuntoVentaInfo(
                        p.getId(), p.getNumero(), p.getDescripcion(), p.getTipoEmision(), p.getDomicilioComercial()
                ))
                .toList();
        List<SistemaInfoDTO.CondicionIvaInfo> condicionesIva = entityManager.createQuery(
                        "SELECT c FROM CondicionIva c WHERE c.fechaBaja IS NULL ORDER BY c.codigoAfip",
                        CondicionIva.class
                ).getResultList().stream()
                .map(c -> new SistemaInfoDTO.CondicionIvaInfo(c.getId(), c.getCodigoAfip(), c.getDenominacion()))
                .toList();
        List<SistemaInfoDTO.MonedaInfo> monedas = entityManager.createQuery(
                        "SELECT m FROM TipoMoneda m WHERE m.fechaBaja IS NULL ORDER BY m.denominacion",
                        TipoMoneda.class
                ).getResultList().stream()
                .map(m -> new SistemaInfoDTO.MonedaInfo(
                        m.getId(), m.getCodigoAfip(), m.getDenominacion(), m.getSimbolo()
                ))
                .toList();
        List<SistemaInfoDTO.ListaPrecioInfo> listasPrecio = entityManager.createQuery(
                        "SELECT l FROM ListaPrecio l WHERE l.fechaBaja IS NULL ORDER BY l.denominacion",
                        ListaPrecio.class
                ).getResultList().stream()
                .map(l -> new SistemaInfoDTO.ListaPrecioInfo(l.getId(), l.getCodigo(), l.getDenominacion()))
                .toList();
        SistemaInfoDTO.UsuarioInfo usuario = entityManager.createQuery(
                        "SELECT u FROM Usuario u WHERE LOWER(u.usuario) = 'admin' ORDER BY u.id",
                        Usuario.class
                ).setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(u -> new SistemaInfoDTO.UsuarioInfo(
                        u.getId(), u.getUsuario(), (u.getNombre() + " " + u.getApellido()).trim()
                ))
                .orElse(null);
        return new SistemaInfoDTO(puntosVenta, condicionesIva, monedas, listasPrecio, usuario);
    }

    @Transactional(readOnly = true)
    public ArticuloAltaDTO.Opciones buscarOpcionesAltaArticulo() {
        List<ArticuloAltaDTO.Opcion> rubros = entityManager.createQuery(
                        "SELECT rubro FROM Rubro rubro WHERE rubro.fechaBaja IS NULL ORDER BY rubro.denominacion",
                        Rubro.class
                ).getResultList().stream()
                .map(rubro -> new ArticuloAltaDTO.Opcion(
                        rubro.getId(),
                        rubro.getCodigo() + " - " + rubro.getDenominacion()
                ))
                .toList();
        List<ArticuloAltaDTO.Opcion> marcas = entityManager.createQuery(
                        "SELECT marca FROM Marca marca WHERE marca.fechaBaja IS NULL ORDER BY marca.denominacion",
                        Marca.class
                ).getResultList().stream()
                .map(marca -> new ArticuloAltaDTO.Opcion(
                        marca.getId(),
                        marca.getCodigo() + " - " + marca.getDenominacion()
                ))
                .toList();
        List<ArticuloAltaDTO.ListaPrecioOpcion> listasPrecio = entityManager.createQuery("""
                        SELECT lista
                        FROM ListaPrecio lista
                        ORDER BY CASE WHEN UPPER(lista.codigo) = 'MINORISTA' THEN 0 ELSE 1 END,
                            lista.denominacion
                        """, ListaPrecio.class)
                .getResultList().stream()
                .map(lista -> new ArticuloAltaDTO.ListaPrecioOpcion(
                        lista.getId(),
                        lista.getCodigo(),
                        lista.getDenominacion()
                ))
                .toList();
        ArticuloAltaDTO.Opcion usuario = entityManager.createQuery(
                        "SELECT usuario FROM Usuario usuario " +
                                "WHERE LOWER(usuario.usuario) = 'admin' ORDER BY usuario.id",
                        Usuario.class
                ).setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(item -> new ArticuloAltaDTO.Opcion(
                        item.getId(),
                        item.getNombre() + " " + item.getApellido() + " (admin)"
                ))
                .orElse(null);
        return new ArticuloAltaDTO.Opciones(rubros, marcas, listasPrecio, usuario);
    }

    private FacturaOpcionesDTO.OpcionDTO opcion(Long id, String descripcion) {
        return new FacturaOpcionesDTO.OpcionDTO(id, descripcion);
    }

    @Transactional
    public EntidadCreadaDTO crearUsuario(DatosMaestrosDTO.UsuarioRequest request) {
        requerido(request.usuario(), "usuario");
        requerido(request.clave(), "clave");
        requerido(request.nombre(), "nombre");
        requerido(request.apellido(), "apellido");

        Usuario entidad = new Usuario();
        entidad.setUsuario(request.usuario().trim());
        entidad.setClave(request.clave());
        entidad.setNombre(request.nombre().trim());
        entidad.setApellido(request.apellido().trim());
        return persistir(entidad, entidad.getUsuario());
    }

    @Transactional
    public EntidadCreadaDTO crearCondicionIva(DatosMaestrosDTO.CondicionIvaRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        CondicionIva entidad = auditar(new CondicionIva(), usuario);
        entidad.setCodigoAfip(request.codigoAfip());
        entidad.setDenominacion(request.denominacion().trim());
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearTipoMoneda(DatosMaestrosDTO.TipoMonedaRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.codigoAfip(), "codigoAfip");
        requerido(request.denominacion(), "denominacion");
        requerido(request.simbolo(), "simbolo");
        TipoMoneda entidad = auditar(new TipoMoneda(), usuario);
        entidad.setCodigoAfip(request.codigoAfip().trim());
        entidad.setDenominacion(request.denominacion().trim());
        entidad.setSimbolo(request.simbolo().trim());
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearPuntoVenta(DatosMaestrosDTO.PuntoVentaRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.descripcion(), "descripcion");
        PuntoVenta entidad = auditar(new PuntoVenta(), usuario);
        entidad.setNumero(request.numero());
        entidad.setDescripcion(request.descripcion().trim());
        entidad.setTipoEmision(request.tipoEmision());
        entidad.setDomicilioComercial(request.domicilioComercial());
        return persistir(entidad, entidad.getDescripcion());
    }

    @Transactional
    public EntidadCreadaDTO crearCliente(DatosMaestrosDTO.ClienteRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        CondicionIva condicionIva = buscar(
                CondicionIva.class,
                request.condicionIvaId(),
                "condicionIvaId"
        );
        TipoDocumento tipoDocumento = tipoDocumento(request.tipoDocumento());
        String documento = documentoValido(request.cuitCuil(), tipoDocumento);
        requerido(request.denominacion(), "denominacion");

        Contacto contacto = new Contacto();
        contacto.setEmail(request.email());
        contacto.setTelefono(request.telefono());
        contacto.setCelular(request.celular());

        Domicilio domicilio = new Domicilio();
        domicilio.setNombreCalle(request.nombreCalle());
        domicilio.setNumeroCalle(request.numeroCalle());

        Cliente entidad = auditar(new Cliente(), usuario);
        entidad.setTipoDocumento(tipoDocumento);
        entidad.setCuitCuil(documento);
        entidad.setDenominacion(request.denominacion().trim());
        entidad.setCondicionIva(condicionIva);
        entidad.setListaPrecioPredeterminada(
                request.listaPrecioId() == null
                        ? buscarListaPrecioMinorista()
                        : buscar(ListaPrecio.class, request.listaPrecioId(), "listaPrecioId")
        );
        entidad.setContacto(contacto);
        entidad.setDomicilio(domicilio);
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO actualizarCliente(
            Long clienteId,
            DatosMaestrosDTO.ClienteRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }
        Cliente cliente = buscar(Cliente.class, clienteId, "clienteId");
        if (cliente.getFechaBaja() != null) {
            throw new IllegalArgumentException("No se puede editar un cliente inactivo");
        }
        Usuario usuario = usuario(request.usuarioId());
        CondicionIva condicionIva = buscar(
                CondicionIva.class,
                request.condicionIvaId(),
                "condicionIvaId"
        );
        TipoDocumento tipoDocumento = tipoDocumento(request.tipoDocumento());
        String documento = documentoValido(request.cuitCuil(), tipoDocumento);
        requerido(request.denominacion(), "denominacion");

        cliente.setTipoDocumento(tipoDocumento);
        cliente.setCuitCuil(documento);
        cliente.setDenominacion(request.denominacion().trim());
        cliente.setCondicionIva(condicionIva);
        cliente.setListaPrecioPredeterminada(
                request.listaPrecioId() == null
                        ? buscarListaPrecioMinorista()
                        : buscar(ListaPrecio.class, request.listaPrecioId(), "listaPrecioId")
        );

        Contacto contacto = cliente.getContacto();
        if (contacto == null) {
            contacto = new Contacto();
            cliente.setContacto(contacto);
        }
        contacto.setEmail(request.email());
        contacto.setTelefono(request.telefono());
        contacto.setCelular(request.celular());

        Domicilio domicilio = cliente.getDomicilio();
        if (domicilio == null) {
            domicilio = new Domicilio();
            cliente.setDomicilio(domicilio);
        }
        domicilio.setNombreCalle(request.nombreCalle());
        domicilio.setNumeroCalle(request.numeroCalle());
        actualizarAuditoria(cliente, usuario);
        entityManager.flush();
        return new EntidadCreadaDTO(cliente.getId(), cliente.getDenominacion());
    }

    private TipoDocumento tipoDocumento(String valor) {
        requerido(valor, "tipoDocumento");
        try {
            return TipoDocumento.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("tipoDocumento debe ser CUIT o CUIL");
        }
    }

    private String documentoValido(String valor, TipoDocumento tipo) {
        requerido(valor, "cuitCuil");
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new IllegalArgumentException(tipo + " debe contener 11 dígitos");
        }

        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int indice = 0; indice < pesos.length; indice++) {
            suma += Character.digit(digitos.charAt(indice), 10) * pesos[indice];
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) verificador = 0;
        if (verificador == 10) verificador = 9;
        if (Character.digit(digitos.charAt(10), 10) != verificador) {
            throw new IllegalArgumentException(tipo + " no tiene un dígito verificador válido");
        }

        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }

    @Transactional
    public void desactivarCliente(Long clienteId, Long usuarioId) {
        Cliente cliente = buscar(Cliente.class, clienteId, "clienteId");
        Usuario usuario = usuario(usuarioId);
        if (cliente.getFechaBaja() == null) {
            marcarComoInactivo(cliente, usuario);
            entityManager.flush();
        }
    }

    @Transactional
    public EntidadCreadaDTO actualizarListaPrecioCliente(
            Long clienteId,
            DatosMaestrosDTO.ClienteListaPrecioRequest request
    ) {
        Cliente cliente = buscar(Cliente.class, clienteId, "clienteId");
        ListaPrecio lista = buscar(ListaPrecio.class, request.listaPrecioId(), "listaPrecioId");
        Usuario usuario = usuario(request.usuarioId());
        cliente.setListaPrecioPredeterminada(lista);
        cliente.setFechaModificacion(new Date());
        cliente.setUsuarioModificacion(usuario);
        return new EntidadCreadaDTO(cliente.getId(), cliente.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearRubro(DatosMaestrosDTO.RubroRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        Rubro entidad = auditar(new Rubro(), usuario);
        entidad.setCodigo(0);
        entidad.setDenominacion(request.denominacion().trim());
        entityManager.persist(entidad);
        entityManager.flush();
        entidad.setCodigo(Math.toIntExact(entidad.getId()));
        return new EntidadCreadaDTO(entidad.getId(), entidad.getDenominacion());
    }

    @Transactional(readOnly = true)
    public PaginaDTO<CatalogoItemDTO> buscarRubros(String busqueda, int pagina, int tamanio, String estado) {
        return buscarCatalogo(Rubro.class, busqueda, pagina, tamanio, estado);
    }

    @Transactional
    public EntidadCreadaDTO actualizarRubro(Long rubroId, DatosMaestrosDTO.RubroRequest request) {
        Rubro rubro = buscar(Rubro.class, rubroId, "rubroId");
        validarActivo(rubro, "rubro");
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        rubro.setDenominacion(request.denominacion().trim());
        actualizarAuditoria(rubro, usuario);
        return new EntidadCreadaDTO(rubro.getId(), rubro.getDenominacion());
    }

    @Transactional
    public void desactivarRubro(Long rubroId, Long usuarioId) {
        Rubro rubro = buscar(Rubro.class, rubroId, "rubroId");
        if (rubro.getFechaBaja() == null) {
            marcarComoInactivo(rubro, usuario(usuarioId));
        }
    }

    @Transactional
    public EntidadCreadaDTO crearMarca(DatosMaestrosDTO.MarcaRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        Marca entidad = auditar(new Marca(), usuario);
        entidad.setCodigo(0);
        entidad.setDenominacion(request.denominacion().trim());
        entityManager.persist(entidad);
        entityManager.flush();
        entidad.setCodigo(Math.toIntExact(entidad.getId()));
        return new EntidadCreadaDTO(entidad.getId(), entidad.getDenominacion());
    }

    @Transactional(readOnly = true)
    public PaginaDTO<CatalogoItemDTO> buscarMarcas(String busqueda, int pagina, int tamanio, String estado) {
        return buscarCatalogo(Marca.class, busqueda, pagina, tamanio, estado);
    }

    @Transactional
    public EntidadCreadaDTO actualizarMarca(Long marcaId, DatosMaestrosDTO.MarcaRequest request) {
        Marca marca = buscar(Marca.class, marcaId, "marcaId");
        validarActivo(marca, "marca");
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        marca.setDenominacion(request.denominacion().trim());
        actualizarAuditoria(marca, usuario);
        return new EntidadCreadaDTO(marca.getId(), marca.getDenominacion());
    }

    @Transactional
    public void desactivarMarca(Long marcaId, Long usuarioId) {
        Marca marca = buscar(Marca.class, marcaId, "marcaId");
        if (marca.getFechaBaja() == null) {
            marcarComoInactivo(marca, usuario(usuarioId));
        }
    }

    @Transactional
    public EntidadCreadaDTO crearListaPrecio(DatosMaestrosDTO.ListaPrecioRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        Long listasConCodigo = entityManager.createQuery(
                        "SELECT COUNT(lista) FROM ListaPrecio lista WHERE UPPER(lista.codigo) = :codigo",
                        Long.class
                ).setParameter("codigo", request.codigo().trim().toUpperCase())
                .getSingleResult();
        if (listasConCodigo > 0) {
            throw new IllegalArgumentException("Ya existe una lista de precios con ese codigo");
        }
        ListaPrecio entidad = auditar(new ListaPrecio(), usuario);
        entidad.setCodigo(request.codigo().trim().toUpperCase());
        entidad.setDenominacion(request.denominacion().trim());
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearArticulo(DatosMaestrosDTO.ArticuloRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        Rubro rubro = buscar(Rubro.class, request.rubroId(), "rubroId");
        Marca marca = request.marcaId() == null
                ? null
                : buscar(Marca.class, request.marcaId(), "marcaId");
        requerido(request.codigo(), "codigo");
        requerido(request.denominacion(), "denominacion");
        Articulo entidad = auditar(new Articulo(), usuario);
        entidad.setCodigo(request.codigo().trim());
        entidad.setDenominacion(request.denominacion().trim());
        entidad.setRubro(rubro);
        entidad.setMarca(marca);
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearArticuloConPrecios(ArticuloAltaDTO.CrearRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }
        requerido(request.codigo(), "codigo");
        requerido(request.denominacion(), "denominacion");
        if (request.precios() == null || request.precios().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una lista de precios");
        }

        Usuario usuario = usuario(request.usuarioId());
        Rubro rubro = buscar(Rubro.class, request.rubroId(), "rubroId");
        Marca marca = request.marcaId() == null
                ? null
                : buscar(Marca.class, request.marcaId(), "marcaId");
        Set<Long> listasUsadas = new HashSet<>();
        for (ArticuloAltaDTO.PrecioRequest precio : request.precios()) {
            if (precio == null || precio.listaPrecioId() == null) {
                throw new IllegalArgumentException("Cada precio debe indicar listaPrecioId");
            }
            if (!listasUsadas.add(precio.listaPrecioId())) {
                throw new IllegalArgumentException("No se puede repetir una lista de precios");
            }
            if (precio.precioVenta() == null || precio.precioVenta().signum() < 0) {
                throw new IllegalArgumentException("precioVenta no puede ser negativo");
            }
        }

        Articulo articulo = auditar(new Articulo(), usuario);
        articulo.setCodigo("PENDIENTE-" + UUID.randomUUID());
        articulo.setDenominacion(request.denominacion().trim());
        articulo.setRubro(rubro);
        articulo.setMarca(marca);
        entityManager.persist(articulo);
        entityManager.flush();
        articulo.setCodigo("ART-" + String.format("%06d", articulo.getId()));

        for (ArticuloAltaDTO.PrecioRequest solicitudPrecio : request.precios()) {
            ListaPrecio lista = buscar(
                    ListaPrecio.class,
                    solicitudPrecio.listaPrecioId(),
                    "listaPrecioId"
            );
            ListaPrecioArticulo precio = auditar(new ListaPrecioArticulo(), usuario);
            precio.setArticulo(articulo);
            precio.setListaPrecio(lista);
            precio.setPrecioVenta(moneda(solicitudPrecio.precioVenta(), "precioVenta"));
            entityManager.persist(precio);
        }
        entityManager.flush();
        return new EntidadCreadaDTO(articulo.getId(), articulo.getDenominacion());
    }

    @Transactional(readOnly = true)
    public ArticuloEdicionDTO obtenerArticuloEdicion(Long articuloId) {
        Articulo articulo = buscar(Articulo.class, articuloId, "articuloId");
        if (articulo.getFechaBaja() != null) {
            throw new IllegalArgumentException("El articulo esta inactivo");
        }
        List<ArticuloEdicionDTO.PrecioDTO> precios = entityManager.createQuery("""
                        SELECT precio
                        FROM ListaPrecioArticulo precio
                        JOIN FETCH precio.listaPrecio lista
                        WHERE precio.articulo = :articulo
                          AND precio.fechaBaja IS NULL
                        ORDER BY lista.denominacion
                        """, ListaPrecioArticulo.class)
                .setParameter("articulo", articulo)
                .getResultList().stream()
                .map(precio -> new ArticuloEdicionDTO.PrecioDTO(
                        precio.getListaPrecio().getId(),
                        precio.getPrecioVenta()
                ))
                .toList();
        return new ArticuloEdicionDTO(
                articulo.getId(),
                articulo.getCodigo(),
                articulo.getDenominacion(),
                articulo.getRubro().getId(),
                articulo.getMarca() == null ? null : articulo.getMarca().getId(),
                precios
        );
    }

    @Transactional
    public EntidadCreadaDTO actualizarArticuloConPrecios(
            Long articuloId,
            ArticuloAltaDTO.CrearRequest request
    ) {
        Articulo articulo = buscar(Articulo.class, articuloId, "articuloId");
        if (articulo.getFechaBaja() != null) {
            throw new IllegalArgumentException("No se puede editar un articulo inactivo");
        }
        validarArticuloConPrecios(request);

        Usuario usuario = usuario(request.usuarioId());
        articulo.setDenominacion(request.denominacion().trim());
        articulo.setRubro(buscar(Rubro.class, request.rubroId(), "rubroId"));
        articulo.setMarca(request.marcaId() == null
                ? null
                : buscar(Marca.class, request.marcaId(), "marcaId"));
        actualizarAuditoria(articulo, usuario);

        List<ListaPrecioArticulo> preciosExistentes = entityManager.createQuery("""
                        SELECT precio
                        FROM ListaPrecioArticulo precio
                        JOIN FETCH precio.listaPrecio
                        WHERE precio.articulo = :articulo
                        """, ListaPrecioArticulo.class)
                .setParameter("articulo", articulo)
                .getResultList();
        Set<Long> listasSeleccionadas = new HashSet<>();

        for (ArticuloAltaDTO.PrecioRequest solicitud : request.precios()) {
            Long listaId = solicitud.listaPrecioId();
            listasSeleccionadas.add(listaId);
            ListaPrecioArticulo precio = preciosExistentes.stream()
                    .filter(item -> item.getListaPrecio().getId().equals(listaId))
                    .findFirst()
                    .orElse(null);
            if (precio == null) {
                precio = auditar(new ListaPrecioArticulo(), usuario);
                precio.setArticulo(articulo);
                precio.setListaPrecio(buscar(ListaPrecio.class, listaId, "listaPrecioId"));
                entityManager.persist(precio);
                preciosExistentes.add(precio);
            } else {
                precio.setFechaBaja(null);
                precio.setUsuarioBaja(null);
                actualizarAuditoria(precio, usuario);
            }
            precio.setPrecioVenta(moneda(solicitud.precioVenta(), "precioVenta"));
        }

        for (ListaPrecioArticulo precio : preciosExistentes) {
            if (!listasSeleccionadas.contains(precio.getListaPrecio().getId())
                    && precio.getFechaBaja() == null) {
                marcarComoInactivo(precio, usuario);
            }
        }
        entityManager.flush();
        return new EntidadCreadaDTO(articulo.getId(), articulo.getDenominacion());
    }

    @Transactional
    public void desactivarArticulo(Long articuloId, Long usuarioId) {
        Articulo articulo = buscar(Articulo.class, articuloId, "articuloId");
        Usuario usuario = usuario(usuarioId);
        if (articulo.getFechaBaja() != null) {
            return;
        }
        marcarComoInactivo(articulo, usuario);
        entityManager.createQuery("""
                        SELECT precio
                        FROM ListaPrecioArticulo precio
                        WHERE precio.articulo = :articulo
                          AND precio.fechaBaja IS NULL
                        """, ListaPrecioArticulo.class)
                .setParameter("articulo", articulo)
                .getResultList()
                .forEach(precio -> marcarComoInactivo(precio, usuario));
        entityManager.flush();
    }

    private void validarArticuloConPrecios(ArticuloAltaDTO.CrearRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }
        requerido(request.denominacion(), "denominacion");
        if (request.precios() == null || request.precios().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una lista de precios");
        }
        Set<Long> listasUsadas = new HashSet<>();
        for (ArticuloAltaDTO.PrecioRequest precio : request.precios()) {
            if (precio == null || precio.listaPrecioId() == null) {
                throw new IllegalArgumentException("Cada precio debe indicar listaPrecioId");
            }
            if (!listasUsadas.add(precio.listaPrecioId())) {
                throw new IllegalArgumentException("No se puede repetir una lista de precios");
            }
            if (precio.precioVenta() == null || precio.precioVenta().signum() < 0) {
                throw new IllegalArgumentException("precioVenta no puede ser negativo");
            }
        }
    }

    @Transactional
    public EntidadCreadaDTO crearPrecio(DatosMaestrosDTO.ListaPrecioArticuloRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        ListaPrecio lista = buscar(ListaPrecio.class, request.listaPrecioId(), "listaPrecioId");
        Articulo articulo = buscar(Articulo.class, request.articuloId(), "articuloId");
        if (request.precioVenta() == null || request.precioVenta().signum() < 0) {
            throw new IllegalArgumentException("precioVenta no puede ser negativo");
        }
        Long preciosExistentes = entityManager.createQuery("""
                SELECT COUNT(precio)
                FROM ListaPrecioArticulo precio
                WHERE precio.listaPrecio.id = :listaPrecioId
                  AND precio.articulo.id = :articuloId
                """, Long.class)
                .setParameter("listaPrecioId", lista.getId())
                .setParameter("articuloId", articulo.getId())
                .getSingleResult();
        if (preciosExistentes > 0) {
            throw new IllegalArgumentException(
                    "El articulo ya tiene un precio definido para esa lista"
            );
        }
        ListaPrecioArticulo entidad = auditar(new ListaPrecioArticulo(), usuario);
        entidad.setListaPrecio(lista);
        entidad.setArticulo(articulo);
        entidad.setPrecioVenta(moneda(request.precioVenta(), "precioVenta"));
        return persistir(entidad, articulo.getDenominacion());
    }

    private Usuario usuario(Long id) {
        return buscar(Usuario.class, id, "usuarioId");
    }

    private <T extends AuditoriaApp> PaginaDTO<CatalogoItemDTO> buscarCatalogo(
            Class<T> tipo,
            String busqueda,
            int pagina,
            int tamanio,
            String estado
    ) {
        int paginaSegura = Math.max(0, pagina);
        int tamanioSeguro = Math.max(1, Math.min(tamanio, 100));
        String nombreEntidad = tipo.getSimpleName();
        String patron = patron(busqueda);
        String filtro = " WHERE " + condicionEstado("item", estado) + " AND (" +
                ":patron = '%%' OR " +
                "LOWER(CAST(item.id AS string)) LIKE :patron OR " +
                "LOWER(CAST(item.codigo AS string)) LIKE :patron OR " +
                "LOWER(item.denominacion) LIKE :patron)";

        List<T> entidades = entityManager.createQuery(
                        "SELECT item FROM " + nombreEntidad + " item" + filtro +
                                " ORDER BY item.denominacion, item.id",
                        tipo
                ).setParameter("patron", patron)
                .setFirstResult(paginaSegura * tamanioSeguro)
                .setMaxResults(tamanioSeguro)
                .getResultList();
        List<CatalogoItemDTO> contenido = entidades.stream()
                .map(item -> {
                    if (item instanceof Rubro rubro) {
                        return new CatalogoItemDTO(
                                rubro.getId(), rubro.getCodigo(), rubro.getDenominacion(),
                                rubro.getFechaAlta(), rubro.getFechaBaja()
                        );
                    }
                    Marca marca = (Marca) item;
                    return new CatalogoItemDTO(
                            marca.getId(), marca.getCodigo(), marca.getDenominacion(),
                            marca.getFechaAlta(), marca.getFechaBaja()
                    );
                })
                .toList();
        long total = entityManager.createQuery(
                        "SELECT COUNT(item.id) FROM " + nombreEntidad + " item" + filtro,
                        Long.class
                ).setParameter("patron", patron)
                .getSingleResult();
        return pagina(contenido, paginaSegura, tamanioSeguro, total);
    }

    private String condicionEstado(String alias, String estado) {
        String valor = estado == null ? "ACTIVOS" : estado.trim().toUpperCase();
        return switch (valor) {
            case "ACTIVOS" -> alias + ".fechaBaja IS NULL";
            case "ELIMINADOS" -> alias + ".fechaBaja IS NOT NULL";
            default -> throw new IllegalArgumentException("estado debe ser ACTIVOS o ELIMINADOS");
        };
    }

    private void validarActivo(AuditoriaApp entidad, String nombre) {
        if (entidad.getFechaBaja() != null) {
            throw new IllegalArgumentException("No se puede editar un " + nombre + " inactivo");
        }
    }

    private <T> T buscar(Class<T> tipo, Long id, String campo) {
        if (id == null) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        T entidad = entityManager.find(tipo, id);
        if (entidad == null) {
            throw new IllegalArgumentException(campo + " no existe: " + id);
        }
        return entidad;
    }

    private <T extends AuditoriaApp> T auditar(T entidad, Usuario usuario) {
        Date ahora = new Date();
        entidad.setFechaAlta(ahora);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioCarga(usuario);
        entidad.setUsuarioModificacion(usuario);
        return entidad;
    }

    private void marcarComoInactivo(AuditoriaApp entidad, Usuario usuario) {
        Date ahora = new Date();
        entidad.setFechaBaja(ahora);
        entidad.setUsuarioBaja(usuario);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioModificacion(usuario);
    }

    private EntidadCreadaDTO persistir(Object entidad, String descripcion) {
        entityManager.persist(entidad);
        entityManager.flush();
        Long id = ((com.example.demo.model.EntityId) entidad).getId();
        return new EntidadCreadaDTO(id, descripcion);
    }

    private void requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
    }

    private BigDecimal moneda(BigDecimal valor, String campo) {
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new PrecisionMonetariaException(campo + " admite como máximo dos decimales");
        }
    }

    private record ArticuloSemilla(
            String codigo,
            String denominacion,
            BigDecimal precioMinorista,
            BigDecimal precioMayorista
    ) {
    }
}
