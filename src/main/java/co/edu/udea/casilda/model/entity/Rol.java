package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rol extends Auditable {
    @Id
    private Integer id;

    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    public Rol(final Integer id, final String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.codigo = nombre == null ? null : nombre.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    public String getCode() {
        return codigo;
    }

    public void setCode(final String code) {
        this.codigo = code;
    }
}
