package com.example.demo.repository;

import com.example.demo.dto.FacturaReporteDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FacturaReporteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<FacturaReporteDTO> buscarFacturasFiltradas(
            Date fechaDesde,
            Date fechaHasta,
            String estado,
            Double montoMinimo
    ) {
        // CONSIGNA - PARTE 2.3: SELECT new proyecta directamente desde JPQL
        // hacia FacturaReporteDTO, sin cargar entidades completas en memoria.
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.example.demo.dto.FacturaReporteDTO(" +
                " f.numero, " +
                " f.fechaEmision, " +
                " COALESCE(c.denominacion, 'Consumidor Final'), " +
                " ci.denominacion, " +
                " pv.descripcion, " +
                " f.importeTotal, " +
                " COUNT(d)" +
                ") " +
                "FROM FacturaVenta f " +
                // LEFT JOIN permite que un cliente nulo se muestre como Consumidor Final.
                "LEFT JOIN f.cliente c " +
                "JOIN f.condicionIva ci " +
                "JOIN f.puntoVenta pv " +
                "JOIN f.detalles d " +
                "WHERE 1 = 1 "
        );

        Map<String, Object> parametros = new HashMap<>();

        // CONSIGNA - PARTE 3.2 y 3.5: construccion dinamica de los cuatro
        // filtros opcionales; solo se agrega a JPQL el filtro recibido.
        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            parametros.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            parametros.put("fechaHasta", fechaHasta);
        }
        if (estado != null && !estado.isBlank()) {
            jpql.append("AND UPPER(f.estado) = UPPER(:estado) ");
            parametros.put("estado", estado.trim());
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            parametros.put("montoMinimo", montoMinimo);
        }

        // CONSIGNA - PARTE 2.3: GROUP BY permite calcular COUNT(d), es decir,
        // la cantidad de detalles de cada factura.
        jpql.append(
                "GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
                "ci.denominacion, pv.descripcion, f.importeTotal " +
                "ORDER BY f.fechaEmision, f.numero"
        );

        TypedQuery<FacturaReporteDTO> query = entityManager.createQuery(
                jpql.toString(),
                FacturaReporteDTO.class
        );
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }
}
