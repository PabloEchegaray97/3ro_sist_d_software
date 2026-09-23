package org.example;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import modelo.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class PoblarBaseDatos {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            limpiarDatos(em);
            insertarDatos(em);
            em.getTransaction().commit();

            System.out.println("Base de datos poblada correctamente.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }

    private static void limpiarDatos(EntityManager em) {
        em.createQuery("DELETE FROM FacturaVentaDetalle d").executeUpdate();
        em.createQuery("DELETE FROM FacturaVenta fv").executeUpdate();
        em.createQuery("DELETE FROM ListaPrecioArticulo lpa").executeUpdate();
        em.createQuery("DELETE FROM ListaPrecio lp").executeUpdate();
        em.createQuery("DELETE FROM Articulo a").executeUpdate();
        em.createQuery("DELETE FROM Cliente c").executeUpdate();
        em.createQuery("DELETE FROM Contacto c").executeUpdate();
        em.createQuery("DELETE FROM Domicilio d").executeUpdate();
        em.createQuery("DELETE FROM TipoMoneda tm").executeUpdate();
        em.createQuery("DELETE FROM PuntoVenta pv").executeUpdate();
        em.createQuery("DELETE FROM Rubro r").executeUpdate();
        em.createQuery("DELETE FROM Marca m").executeUpdate();
        em.createQuery("DELETE FROM CondicionIva ci").executeUpdate();
        em.createQuery("DELETE FROM Usuario u").executeUpdate();
    }

    private static void insertarDatos(EntityManager em) {
        Date fechaAlta = crearFecha(2026, Calendar.SEPTEMBER, 1);

        Usuario admin = crearUsuario("admin", "1234", "Juan", "Perez");
        Usuario vendedor = crearUsuario("vendedor", "1234", "Ana", "Gomez");
        Usuario supervisor = crearUsuario("supervisor", "1234", "Mario", "Lopez");
        em.persist(admin);
        em.persist(vendedor);
        em.persist(supervisor);

        Rubro electronica = auditar(new Rubro(), admin, fechaAlta);
        electronica.setCodigo(1);
        electronica.setDenominacion("Electronica");

        Rubro libreria = auditar(new Rubro(), admin, fechaAlta);
        libreria.setCodigo(2);
        libreria.setDenominacion("Libreria");

        Marca logitech = auditar(new Marca(), admin, fechaAlta);
        logitech.setCodigo(1);
        logitech.setDenominacion("Logitech");

        Marca samsung = auditar(new Marca(), admin, fechaAlta);
        samsung.setCodigo(2);
        samsung.setDenominacion("Samsung");

        Marca generica = auditar(new Marca(), admin, fechaAlta);
        generica.setCodigo(3);
        generica.setDenominacion("Generica");

        em.persist(electronica);
        em.persist(libreria);
        em.persist(logitech);
        em.persist(samsung);
        em.persist(generica);

        PuntoVenta pv1 = crearPuntoVenta(1, "Casa Central", admin, fechaAlta);
        PuntoVenta pv2 = crearPuntoVenta(2, "Sucursal Norte", admin, fechaAlta);
        PuntoVenta pv5 = crearPuntoVenta(5, "Sucursal Sur", admin, fechaAlta);
        PuntoVenta pv10 = crearPuntoVenta(10, "Sucursal Centro", admin, fechaAlta);
        em.persist(pv1);
        em.persist(pv2);
        em.persist(pv5);
        em.persist(pv10);

        TipoMoneda pesos = auditar(new TipoMoneda(), admin, fechaAlta);
        pesos.setCodigoAfip("PES");
        pesos.setDenominacion("Peso Argentino");
        pesos.setSimbolo("$");
        em.persist(pesos);

        Cliente clienteMayorista = crearCliente(
                "20-12345678-9",
                "Cliente Mayorista",
                "mayorista@example.com",
                "2614000001",
                "San Martin",
                "100",
                admin,
                fechaAlta
        );

        Cliente clienteMinorista = crearCliente(
                "27-87654321-0",
                "Cliente Minorista",
                "minorista@example.com",
                "2614000002",
                "Belgrano",
                "250",
                admin,
                fechaAlta
        );

        Cliente empresaNorte = crearCliente(
                "30-11111111-1",
                "Empresa Norte",
                "norte@example.com",
                "2614000003",
                "Colon",
                "500",
                admin,
                fechaAlta
        );

        em.persist(clienteMayorista);
        em.persist(clienteMinorista);
        em.persist(empresaNorte);

        ListaPrecio listaGeneral = auditar(new ListaPrecio(), admin, fechaAlta);
        listaGeneral.setCodigo("LP-GEN");
        listaGeneral.setDenominacion("Lista General");
        em.persist(listaGeneral);

        Articulo teclado = crearArticulo("ART01", "Teclado Mecanico", electronica, logitech, admin, fechaAlta);
        Articulo mouse = crearArticulo("ART02", "Mouse Inalambrico", electronica, logitech, admin, fechaAlta);
        Articulo monitor = crearArticulo("ART03", "Monitor 24 pulgadas", electronica, samsung, admin, fechaAlta);
        Articulo cable = crearArticulo("ART04", "Cable HDMI", electronica, generica, admin, fechaAlta);
        Articulo cuaderno = crearArticulo("ART05", "Cuaderno A4", libreria, null, admin, fechaAlta);
        em.persist(teclado);
        em.persist(mouse);
        em.persist(monitor);
        em.persist(cable);
        em.persist(cuaderno);

        ListaPrecioArticulo lpaTeclado = crearListaPrecioArticulo(listaGeneral, teclado, 75000.0, admin, fechaAlta);
        ListaPrecioArticulo lpaMouse = crearListaPrecioArticulo(listaGeneral, mouse, 12500.0, admin, fechaAlta);
        ListaPrecioArticulo lpaMonitor = crearListaPrecioArticulo(listaGeneral, monitor, 45000.0, admin, fechaAlta);
        ListaPrecioArticulo lpaCable = crearListaPrecioArticulo(listaGeneral, cable, 4000.0, admin, fechaAlta);
        em.persist(lpaTeclado);
        em.persist(lpaMouse);
        em.persist(lpaMonitor);
        em.persist(lpaCable);

        FacturaVenta f1001 = crearFactura(1001L, crearFecha(2026, Calendar.SEPTEMBER, 2), pv10,
                clienteMayorista, pesos, admin, "EMITIDA", 75000.0, null);
        agregarDetalle(f1001, lpaTeclado, 1, 75000.0);

        FacturaVenta f1002 = crearFactura(1002L, crearFecha(2026, Calendar.SEPTEMBER, 3), pv10,
                clienteMinorista, pesos, admin, "EMITIDA", 25000.0, null);
        agregarDetalle(f1002, lpaMouse, 2, 12500.0);

        FacturaVenta f1003 = crearFactura(1003L, crearFecha(2026, Calendar.SEPTEMBER, 4), pv1,
                clienteMayorista, pesos, admin, "EMITIDA", 8000.0, null);
        agregarDetalle(f1003, lpaCable, 2, 4000.0);

        FacturaVenta f1004 = crearFactura(1004L, crearFecha(2026, Calendar.SEPTEMBER, 5), pv2,
                empresaNorte, pesos, admin, "ANULADA", 12500.0, crearFecha(2026, Calendar.SEPTEMBER, 6));
        agregarDetalle(f1004, lpaMouse, 1, 12500.0);

        FacturaVenta f1005 = crearFactura(1005L, crearFecha(2026, Calendar.SEPTEMBER, 6), pv5,
                clienteMinorista, pesos, admin, "PENDIENTE", 45000.0, null);
        agregarDetalle(f1005, lpaMonitor, 1, 45000.0);

        FacturaVenta f1006 = crearFactura(1006L, crearFecha(2026, Calendar.SEPTEMBER, 7), pv10,
                clienteMayorista, pesos, admin, "EMITIDA", 90000.0, null);
        agregarDetalle(f1006, lpaMonitor, 1, 45000.0);
        agregarDetalle(f1006, lpaMouse, 2, 12500.0);
        agregarDetalle(f1006, lpaCable, 5, 4000.0);

        FacturaVenta f1007 = crearFactura(1007L, crearFecha(2026, Calendar.SEPTEMBER, 8), pv1,
                empresaNorte, pesos, vendedor, "EMITIDA", 16500.0, null);
        agregarDetalle(f1007, lpaMouse, 1, 12500.0);
        agregarDetalle(f1007, lpaCable, 1, 4000.0);

        em.persist(f1001);
        em.persist(f1002);
        em.persist(f1003);
        em.persist(f1004);
        em.persist(f1005);
        em.persist(f1006);
        em.persist(f1007);
    }

    private static Usuario crearUsuario(String usuario, String clave, String nombre, String apellido) {
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setUsuario(usuario);
        nuevoUsuario.setClave(clave);
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setApellido(apellido);
        return nuevoUsuario;
    }

    private static PuntoVenta crearPuntoVenta(int numero, String descripcion, Usuario usuario, Date fechaAlta) {
        PuntoVenta puntoVenta = auditar(new PuntoVenta(), usuario, fechaAlta);
        puntoVenta.setNumero(numero);
        puntoVenta.setDescripcion(descripcion);
        puntoVenta.setTipoEmision("ONLINE");
        puntoVenta.setDomicilioComercial("Mendoza");
        return puntoVenta;
    }

    private static Cliente crearCliente(String cuitCuil, String denominacion, String email, String telefono,
                                        String calle, String numeroCalle, Usuario usuario, Date fechaAlta) {
        Contacto contacto = new Contacto();
        contacto.setEmail(email);
        contacto.setTelefono(telefono);

        Domicilio domicilio = new Domicilio();
        domicilio.setNombreCalle(calle);
        domicilio.setNumeroCalle(numeroCalle);

        Cliente cliente = auditar(new Cliente(), usuario, fechaAlta);
        cliente.setCuitCuil(cuitCuil);
        cliente.setDenominacion(denominacion);
        cliente.setContacto(contacto);
        cliente.setDomicilio(domicilio);
        return cliente;
    }

    private static Articulo crearArticulo(String codigo, String denominacion, Rubro rubro, Marca marca,
                                          Usuario usuario, Date fechaAlta) {
        Articulo articulo = auditar(new Articulo(), usuario, fechaAlta);
        articulo.setCodigo(codigo);
        articulo.setDenominacion(denominacion);
        articulo.setRubro(rubro);
        articulo.setMarca(marca);
        return articulo;
    }

    private static ListaPrecioArticulo crearListaPrecioArticulo(ListaPrecio listaPrecio, Articulo articulo,
                                                                double precioVenta, Usuario usuario, Date fechaAlta) {
        ListaPrecioArticulo listaPrecioArticulo = auditar(new ListaPrecioArticulo(), usuario, fechaAlta);
        listaPrecioArticulo.setListaPrecio(listaPrecio);
        listaPrecioArticulo.setArticulo(articulo);
        listaPrecioArticulo.setPrecioVenta(precioVenta);
        return listaPrecioArticulo;
    }

    private static FacturaVenta crearFactura(Long numero, Date fechaEmision, PuntoVenta puntoVenta, Cliente cliente,
                                             TipoMoneda tipoMoneda, Usuario usuarioCarga, String estado,
                                             double importeTotal, Date fechaAnulacion) {
        FacturaVenta factura = auditar(new FacturaVenta(), usuarioCarga, fechaEmision);
        factura.setNumero(numero);
        factura.setFechaEmision(fechaEmision);
        factura.setPuntoVenta(puntoVenta);
        factura.setCliente(cliente);
        factura.setTipoMoneda(tipoMoneda);
        factura.setEstado(estado);
        factura.setImporteTotal(importeTotal);
        factura.setImporteCobrado("EMITIDA".equals(estado) ? importeTotal : 0.0);
        factura.setImporteSaldo("EMITIDA".equals(estado) ? 0.0 : importeTotal);
        factura.setFechaAnulacion(fechaAnulacion);
        factura.setDetalles(new ArrayList<>());
        return factura;
    }

    private static void agregarDetalle(FacturaVenta factura, ListaPrecioArticulo listaPrecioArticulo,
                                       double cantidad, double precioUnitario) {
        double subtotal = cantidad * precioUnitario;

        FacturaVentaDetalle detalle = new FacturaVentaDetalle();
        detalle.setFactura(factura);
        detalle.setListaPrecioArticulo(listaPrecioArticulo);
        detalle.setDescripcion(listaPrecioArticulo.getArticulo().getDenominacion());
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setImporteNeto(subtotal);
        detalle.setImporteSubtotal(subtotal);

        factura.getDetalles().add(detalle);
    }

    private static <T extends AuditoriaApp> T auditar(T entidad, Usuario usuario, Date fecha) {
        entidad.setFechaAlta(fecha);
        entidad.setFechaModificacion(fecha);
        entidad.setUsuarioCarga(usuario);
        entidad.setUsuarioModificacion(usuario);
        return entidad;
    }

    private static Date crearFecha(int anio, int mes, int dia) {
        Calendar calendario = Calendar.getInstance();
        calendario.set(anio, mes, dia, 0, 0, 0);
        calendario.set(Calendar.MILLISECOND, 0);
        return calendario.getTime();
    }
}
