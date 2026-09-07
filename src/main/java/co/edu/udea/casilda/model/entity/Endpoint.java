package co.edu.udea.casilda.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "endpoint",
        uniqueConstraints = @UniqueConstraint(columnNames = {"path", "http_method"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean publico = false;

    public String getMethod() {
        return httpMethod;
    }

    public void setMethod(final String method) {
        this.httpMethod = method;
    }

    public String getRuta() {
        return path;
    }

    public void setRuta(final String ruta) {
        this.path = ruta;
    }

    public String getMetodo() {
        return httpMethod;
    }

    public void setMetodo(final String metodo) {
        this.httpMethod = metodo;
    }
}
