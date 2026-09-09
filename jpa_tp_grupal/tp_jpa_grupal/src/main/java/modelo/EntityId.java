package modelo;

import jakarta.persistence.*;

// TODO: Agregar anotación @MappedSuperclass
@MappedSuperclass
public abstract class EntityId {

    // TODO: Agregar @Id y @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
}