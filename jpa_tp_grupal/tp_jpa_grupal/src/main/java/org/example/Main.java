package org.example;

import jakarta.persistence.*;
import modelo.*;
import java.util.ArrayList;
import java.util.Date;

public class Main {
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

            // como todas las entidades heredan de AuditoriaApp y exigen un Usuario, creamos uno primero
            Usuario usuarioAudit = new Usuario();
            usuarioAudit.setUsuario("admin");
            usuarioAudit.setClave("1234");
            usuarioAudit.setNombre("Juan");
            usuarioAudit.setApellido("Perez");
            em.persist(usuarioAudit); // lo guardamos para poder usarlo en las fechas de auditoría

            // crear un Punto de Venta
            PuntoVenta pv = new PuntoVenta();
            pv.setNumero(10);
            pv.setFechaAlta(ahora);
            pv.setFechaModificacion(ahora);
            pv.setUsuarioCarga(usuarioAudit);
            pv.setUsuarioModificacion(usuarioAudit);
            em.persist(pv);

            // crear los objetos de Artículo y Lista de Precio
            Articulo articulo = new Articulo();
            articulo.setCodigo("ART01");
            articulo.setDenominacion("Teclado Mecánico");
            articulo.setFechaAlta(ahora);
            articulo.setFechaModificacion(ahora);
            articulo.setUsuarioCarga(usuarioAudit);
            articulo.setUsuarioModificacion(usuarioAudit);
            em.persist(articulo);

            ListaPrecio lp = new ListaPrecio();
            lp.setCodigo("LP-GEN");
            lp.setDenominacion("Lista General");
            lp.setFechaAlta(ahora);
            lp.setFechaModificacion(ahora);
            lp.setUsuarioCarga(usuarioAudit);
            lp.setUsuarioModificacion(usuarioAudit);
            em.persist(lp);

            ListaPrecioArticulo lpa = new ListaPrecioArticulo();
            lpa.setListaPrecio(lp);
            lpa.setArticulo(articulo);
            lpa.setPrecioVenta(50000.0);
            lpa.setFechaAlta(ahora);
            lpa.setFechaModificacion(ahora);
            lpa.setUsuarioCarga(usuarioAudit);
            lpa.setUsuarioModificacion(usuarioAudit);
            em.persist(lpa);

            // =================================================================
            // 3. armar la Factura y sus Detalles (Prueba de Cascada)
            // =================================================================

            // Crear la cabecera de FacturaVenta
            FacturaVenta factura = new FacturaVenta();
            factura.setFechaEmision(ahora);
            factura.setPuntoVenta(pv);
            factura.setImporteTotal(50000.0);
            factura.setEstado("EMITIDA");
            // campos de auditoría
            factura.setFechaAlta(ahora);
            factura.setFechaModificacion(ahora);
            factura.setUsuarioCarga(usuarioAudit);
            factura.setUsuarioModificacion(usuarioAudit);

            // crear el ítem FacturaVentaDetalle
            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(lpa);
            detalle.setCantidad(1);
            detalle.setPrecioUnitario(50000.0);
            detalle.setImporteSubtotal(50000.0);

            // asociarlos bidireccionalmente
            detalle.setFactura(factura); // el detalle conoce a su cabecera
            factura.setDetalles(new ArrayList<>());
            factura.getDetalles().add(detalle); // la cabecera conoce a su detalle

            // REQUISITO CLAVE: persistir únicamente la cabecera

            System.out.println("Guardando la factura...");
            em.persist(factura);

            // finalizar la transacción
            em.getTransaction().commit();
            System.out.println("La transacción fué exitosa! Revisá la base de datos");

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