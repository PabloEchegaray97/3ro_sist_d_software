package modelo;

import jakarta.persistence.*;

// TODO: Agregar @Entity y @Table
@Entity
@Table(name = "lista_precio_articulo")
public class ListaPrecioArticulo extends AuditoriaApp {

    // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false)
    @ManyToOne
    @JoinColumn(nullable = false)
    private ListaPrecio listaPrecio;

    // TODO: Configurar @Column(nullable = false)
    @Column(nullable = false)
    private double precioVenta;

    // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false)
    @ManyToOne
    @JoinColumn(nullable = false)
    private Articulo articulo;

    public ListaPrecio getListaPrecio() {
        return listaPrecio;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public void setListaPrecio(ListaPrecio listaPrecio) {
        this.listaPrecio = listaPrecio;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public void setArticulo(Articulo articulo) {
        this.articulo = articulo;
    }
}