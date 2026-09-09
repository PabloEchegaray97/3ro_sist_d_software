package modelo;

import jakarta.persistence.*;

// TODO: Agregar @Entity y @Table
@Entity
@Table(name = "articulo")
public class Articulo extends AuditoriaApp {

    // TODO: Configurar @ManyToOne
    @ManyToOne
    private Rubro rubro;

    // TODO: Configurar @Column(nullable = false) en codigo y denominacion
    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String denominacion;

    // TODO: Configurar @ManyToOne
    @ManyToOne
    private Marca marca;

    public Rubro getRubro() {
        return rubro;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setRubro(Rubro rubro) {
        this.rubro = rubro;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }
}