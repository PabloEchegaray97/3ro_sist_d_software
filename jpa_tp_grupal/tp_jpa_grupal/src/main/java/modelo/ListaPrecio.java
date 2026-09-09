package modelo;

import jakarta.persistence.*;

// TODO: Agregar @Entity y @Table
@Entity
@Table(name = "lista_precio")
public class ListaPrecio extends AuditoriaApp {

    // TODO: Configurar @Column(nullable = false) en codigo y denominacion
    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String denominacion;

    public String getCodigo() {
        return codigo;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }
}