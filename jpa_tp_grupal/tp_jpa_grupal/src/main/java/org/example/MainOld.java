package org.example;

import jakarta.persistence.*;
import modelo.*;
import java.util.ArrayList;
import java.util.Date;

public class MainOld {
    public static void main(String[] args) {

        // Iniciar el contenedor de JPA mediante Persistence y obtener el EntityManager
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            // iniciar una transaccion
            em.getTransaction().begin();

            // ===============================================================
            // 1. instanciar objetos necesarios (Dependencias Previas)
            // ===============================================================
            Date ahora = new Date();

            // como todas las entidades heredan de AuditoriaApp y exigen un Usuario, lo incluimos en el grafo
            Usuario usuarioAudit = new Usuario();
            usuarioAudit.setUsuario("admin");
            usuarioAudit.setClave("1234");
            usuarioAudit.setNombre("Juan");
            usuarioAudit.setApellido("Perez");
            // crear un Punto de Venta
            PuntoVenta pv = new PuntoVenta();
            pv.setNumero(10);
            pv.setFechaAlta(ahora);
            pv.setFechaModificacion(ahora);
            pv.setUsuarioCarga(usuarioAudit);
            pv.setUsuarioModificacion(usuarioAudit);
            // crear los objetos de Articulo y Lista de Precio
            Articulo articulo = new Articulo();
            articulo.setCodigo("ART01");
            articulo.setDenominacion("Teclado Mecanico");
            articulo.setFechaAlta(ahora);
            articulo.setFechaModificacion(ahora);
            articulo.setUsuarioCarga(usuarioAudit);
            articulo.setUsuarioModificacion(usuarioAudit);
            ListaPrecio lp = new ListaPrecio();
            lp.setCodigo("LP-GEN");
            lp.setDenominacion("Lista General");
            lp.setFechaAlta(ahora);
            lp.setFechaModificacion(ahora);
            lp.setUsuarioCarga(usuarioAudit);
            lp.setUsuarioModificacion(usuarioAudit);
            ListaPrecioArticulo lpa = new ListaPrecioArticulo();
            lpa.setListaPrecio(lp);
            lpa.setArticulo(articulo);
            lpa.setPrecioVenta(50000.0);
            lpa.setFechaAlta(ahora);
            lpa.setFechaModificacion(ahora);
            lpa.setUsuarioCarga(usuarioAudit);
            lpa.setUsuarioModificacion(usuarioAudit);

            // crear el Cliente y sus dependencias
            Contacto contacto = new Contacto();
            contacto.setEmail("cliente@example.com");
            contacto.setTelefono("123456789");

            Domicilio domicilio = new Domicilio();
            domicilio.setNombreCalle("Av. Siempre Viva");
            domicilio.setNumeroCalle("742");

            Cliente cliente = new Cliente();
            cliente.setCuitCuil("20-12345678-9");
            cliente.setDenominacion("Cliente de prueba");
            cliente.setContacto(contacto);
            cliente.setDomicilio(domicilio);
            cliente.setFechaAlta(ahora);
            cliente.setFechaModificacion(ahora);
            cliente.setUsuarioCarga(usuarioAudit);
            cliente.setUsuarioModificacion(usuarioAudit);

            // crear el tipo de moneda
            TipoMoneda tipoMoneda = new TipoMoneda();
            tipoMoneda.setCodigoAfip("PES");
            tipoMoneda.setDenominacion("Peso Argentino");
            tipoMoneda.setSimbolo("$");
            tipoMoneda.setFechaAlta(ahora);
            tipoMoneda.setFechaModificacion(ahora);
            tipoMoneda.setUsuarioCarga(usuarioAudit);
            tipoMoneda.setUsuarioModificacion(usuarioAudit);

            // =================================================================
            // 3. armar la Factura y sus Detalles (Prueba de Cascada)
            // =================================================================

            // Crear la cabecera de FacturaVenta
            FacturaVenta factura = new FacturaVenta();
            factura.setFechaEmision(ahora);
            factura.setPuntoVenta(pv);
            factura.setCliente(cliente);
            factura.setTipoMoneda(tipoMoneda);
            factura.setImporteTotal(50000.0);
            factura.setEstado("EMITIDA");
            // campos de auditoria
            factura.setFechaAlta(ahora);
            factura.setFechaModificacion(ahora);
            factura.setUsuarioCarga(usuarioAudit);
            factura.setUsuarioModificacion(usuarioAudit);

            // crear el item FacturaVentaDetalle
            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(lpa);
            detalle.setCantidad(1);
            detalle.setPrecioUnitario(50000.0);
            detalle.setImporteSubtotal(50000.0);

            // asociarlos bidireccionalmente
            detalle.setFactura(factura); // el detalle conoce a su cabecera
            factura.setDetalles(new ArrayList<>());
            factura.getDetalles().add(detalle); // la cabecera conoce a su detalle

            // REQUISITO CLAVE: persistir unicamente la cabecera

            System.out.println("Guardando la factura...");
            em.persist(factura);

            // finalizar la transaccion
            em.getTransaction().commit();
            System.out.println("La transaccion fue exitosa! Revisa la base de datos");

        } catch (Exception e) {
            // si algo falla, deshacemos los cambios
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            // cerrar EntityManager y EntityManagerFactory
            em.close();
            emf.close();
        }
    }
}
