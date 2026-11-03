package com.example.demo.repository;

import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.model.EntityId;
import com.example.demo.model.FacturaVenta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;
import com.example.demo.model.PuntoVenta;
import org.springframework.stereotype.Repository;

@Repository
public class FacturaEscrituraRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public <T extends EntityId> T buscarRequerido(Class<T> tipo, Long id, String nombreCampo) {
        if (id == null) {
            throw new IllegalArgumentException(nombreCampo + " es obligatorio");
        }
        T entidad = entityManager.find(tipo, id);
        if (entidad == null) {
            throw new RecursoNoEncontradoException(nombreCampo + " no existe: " + id);
        }
        return entidad;
    }

    // CONSIGNA ESCRITURA - 2.c y 2.d: localiza la factura que se actualiza o elimina.
    public FacturaVenta buscarFactura(Long id) {
        FacturaVenta factura = entityManager.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new RecursoNoEncontradoException("Factura no encontrada: " + id);
        }
        return factura;
    }

    public FacturaVenta buscarFacturaParaActualizar(Long id) {
        FacturaVenta factura = entityManager.find(FacturaVenta.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (factura == null) {
            throw new RecursoNoEncontradoException("Factura no encontrada: " + id);
        }
        return factura;
    }

    public PuntoVenta bloquearPuntoVenta(Long id) {
        PuntoVenta puntoVenta = entityManager.find(PuntoVenta.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (puntoVenta == null) {
            throw new RecursoNoEncontradoException("Punto de venta no encontrado: " + id);
        }
        return puntoVenta;
    }

    public boolean existeNumero(Long numero) {
        if (numero == null) return false;
        Long cantidad = entityManager.createQuery(
                "SELECT COUNT(f) FROM FacturaVenta f WHERE f.numero = :numero",
                Long.class
        ).setParameter("numero", numero).getSingleResult();
        return cantidad > 0;
    }

    public boolean existeNumeroEnOtraFactura(Long numero, Long facturaId) {
        if (numero == null) return false;
        Long cantidad = entityManager.createQuery(
                "SELECT COUNT(f) FROM FacturaVenta f " +
                "WHERE f.numero = :numero AND f.id <> :facturaId",
                Long.class
        ).setParameter("numero", numero)
                .setParameter("facturaId", facturaId)
                .getSingleResult();
        return cantidad > 0;
    }

    public void guardar(FacturaVenta factura) {
        if (factura.getId() == null) {
            entityManager.persist(factura);
        }
        entityManager.flush();
    }

    public void eliminar(FacturaVenta factura) {
        entityManager.remove(factura);
        entityManager.flush();
    }
}
