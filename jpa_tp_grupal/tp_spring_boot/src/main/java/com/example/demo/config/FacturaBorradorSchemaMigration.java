package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Corrige bases creadas con la versión anterior del modelo, donde
 * factura_venta.fecha_emision era NOT NULL. Un borrador no tiene fecha fiscal
 * hasta que ARCA lo autoriza, por lo que esa columna debe admitir NULL.
 */
@Component
@RequiredArgsConstructor
public class FacturaBorradorSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String nullable = jdbcTemplate.query(
                """
                SELECT IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'factura_venta'
                  AND COLUMN_NAME = 'fecha_emision'
                """,
                resultSet -> resultSet.next() ? resultSet.getString(1) : null
        );

        if ("NO".equalsIgnoreCase(nullable)) {
            jdbcTemplate.execute(
                    "ALTER TABLE factura_venta MODIFY COLUMN fecha_emision DATETIME(6) NULL"
            );
        }
    }
}
