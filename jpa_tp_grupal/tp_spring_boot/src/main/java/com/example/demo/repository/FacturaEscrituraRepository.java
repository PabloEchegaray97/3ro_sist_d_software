package com.example.demo.repository;

import com.example.demo.model.EntityId;
import com.example.demo.model.FacturaVenta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
            throw new IllegalArgumentException(nombreCampo + " no existe: " + id);
        }
        return entidad;
    }

    public boolean existeNumero(Long numero) {
        Long cantidad = entityManager.createQuery(
                "SELECT COUNT(f) FROM FacturaVenta f WHERE f.numero = :numero",
                Long.class
        ).setParameter("numero", numero).getSingleResult();
        return cantidad > 0;
    }

    public void guardar(FacturaVenta factura) {
        entityManager.persist(factura);
        entityManager.flush();
    }
}
