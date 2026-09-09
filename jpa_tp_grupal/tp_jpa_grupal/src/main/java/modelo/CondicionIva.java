package modelo;

import jakarta.persistence.*;

// TODO: Agregar @Entity y @Table
@Entity
@Table(name = "condicion_iva")
public class CondicionIva extends AuditoriaApp {

    // TODO: Configurar @Column(nullable = false) en codigoAfip y denominacion
    @Column(nullable = false)
    private int codigoAfip;

    @Column(nullable = false)
    private String denominacion;
}