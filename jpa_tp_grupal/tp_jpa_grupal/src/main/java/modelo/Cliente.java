package modelo;

import jakarta.persistence.*;

// TODO: Agregar @Entity y @Table
@Entity
@Table(name = "cliente")
public class Cliente extends AuditoriaApp {

    // TODO: Configurar @Column(nullable = false) en cuitCuil y denominacion
    @Column(nullable = false)
    private String cuitCuil;

    @Column(nullable = false)
    private String denominacion;

    // TODO: Configurar @OneToOne y @JoinColumn (nullable = false)
    @OneToOne
    @JoinColumn(nullable = false)
    private Contacto contacto;

    // TODO: Configurar @OneToOne y @JoinColumn(nullable = false)
    @OneToOne
    @JoinColumn(nullable = false)
    private Domicilio domicilio;

    public String getCuitCuil() {
        return cuitCuil;
    }

    public void setCuitCuil(String cuitCuil) {
        this.cuitCuil = cuitCuil;
    }

    public Contacto getContacto() {
        return contacto;
    }

    public void setContacto(Contacto contacto) {
        this.contacto = contacto;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public Domicilio getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(Domicilio domicilio) {
        this.domicilio = domicilio;
    }
}