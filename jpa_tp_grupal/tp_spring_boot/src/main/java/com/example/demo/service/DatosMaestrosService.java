package com.example.demo.service;

import com.example.demo.dto.DatosMaestrosDTO;
import com.example.demo.dto.EntidadCreadaDTO;
import com.example.demo.dto.ArticuloDisponibleDTO;
import com.example.demo.dto.FacturaOpcionesDTO;
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
import com.example.demo.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

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

    @Transactional(readOnly = true)
    public List<ArticuloDisponibleDTO> buscarArticulosDisponibles() {
        return entityManager.createQuery("""
                SELECT new com.example.demo.dto.ArticuloDisponibleDTO(
                    precio.id,
                    articulo.id,
                    articulo.codigo,
                    articulo.denominacion,
                    lista.denominacion,
                    precio.precioVenta
                )
                FROM ListaPrecioArticulo precio
                JOIN precio.articulo articulo
                JOIN precio.listaPrecio lista
                ORDER BY articulo.denominacion, lista.denominacion
                """, ArticuloDisponibleDTO.class).getResultList();
    }

    @Transactional(readOnly = true)
    public FacturaOpcionesDTO buscarOpcionesFactura() {
        Long siguienteNumero = entityManager.createQuery(
                "SELECT COALESCE(MAX(f.numero), 0) + 1 FROM FacturaVenta f",
                Long.class
        ).getSingleResult();

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
                        c.getCondicionIva() == null ? null : c.getCondicionIva().getId()
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

        FacturaOpcionesDTO.OpcionDTO usuario = entityManager.createQuery(
                        "SELECT u FROM Usuario u WHERE LOWER(u.usuario) = 'admin' ORDER BY u.id",
                        Usuario.class
                ).setMaxResults(1)
                .getResultList().stream()
                .findFirst()
                .map(u -> opcion(u.getId(), u.getNombre() + " " + u.getApellido() + " (admin)"))
                .orElse(null);

        return new FacturaOpcionesDTO(
                siguienteNumero,
                puntosVenta,
                clientes,
                condicionesIva,
                tiposMoneda,
                usuario
        );
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
        requerido(request.cuitCuil(), "cuitCuil");
        requerido(request.denominacion(), "denominacion");

        Contacto contacto = new Contacto();
        contacto.setEmail(request.email());
        contacto.setTelefono(request.telefono());
        contacto.setCelular(request.celular());

        Domicilio domicilio = new Domicilio();
        domicilio.setNombreCalle(request.nombreCalle());
        domicilio.setNumeroCalle(request.numeroCalle());

        Cliente entidad = auditar(new Cliente(), usuario);
        entidad.setCuitCuil(request.cuitCuil().trim());
        entidad.setDenominacion(request.denominacion().trim());
        entidad.setCondicionIva(condicionIva);
        entidad.setContacto(contacto);
        entidad.setDomicilio(domicilio);
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearRubro(DatosMaestrosDTO.RubroRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        Rubro entidad = auditar(new Rubro(), usuario);
        entidad.setCodigo(request.codigo());
        entidad.setDenominacion(request.denominacion().trim());
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearMarca(DatosMaestrosDTO.MarcaRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.denominacion(), "denominacion");
        Marca entidad = auditar(new Marca(), usuario);
        entidad.setCodigo(request.codigo());
        entidad.setDenominacion(request.denominacion().trim());
        return persistir(entidad, entidad.getDenominacion());
    }

    @Transactional
    public EntidadCreadaDTO crearListaPrecio(DatosMaestrosDTO.ListaPrecioRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        requerido(request.codigo(), "codigo");
        requerido(request.denominacion(), "denominacion");
        ListaPrecio entidad = auditar(new ListaPrecio(), usuario);
        entidad.setCodigo(request.codigo().trim());
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
    public EntidadCreadaDTO crearPrecio(DatosMaestrosDTO.ListaPrecioArticuloRequest request) {
        Usuario usuario = usuario(request.usuarioId());
        ListaPrecio lista = buscar(ListaPrecio.class, request.listaPrecioId(), "listaPrecioId");
        Articulo articulo = buscar(Articulo.class, request.articuloId(), "articuloId");
        if (request.precioVenta() < 0) {
            throw new IllegalArgumentException("precioVenta no puede ser negativo");
        }
        ListaPrecioArticulo entidad = auditar(new ListaPrecioArticulo(), usuario);
        entidad.setListaPrecio(lista);
        entidad.setArticulo(articulo);
        entidad.setPrecioVenta(request.precioVenta());
        return persistir(entidad, articulo.getDenominacion());
    }

    private Usuario usuario(Long id) {
        return buscar(Usuario.class, id, "usuarioId");
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
}
