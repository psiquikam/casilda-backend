package co.edu.udea.casilda.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entidad Usuario - Representa un usuario del sistema con acceso a la plataforma.
 * Implementa UserDetails de Spring Security.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Column(name = "fechacreacion", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaCreacion;

    @Column(name = "fechaactualizacion", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idrol", nullable = false)
    private Rol rol;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuariorol",
            joinColumns = @JoinColumn(name = "idusuario"),
            inverseJoinColumns = @JoinColumn(name = "idrol"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"idusuario", "idrol"})
    )
    private Set<Rol> roles = new LinkedHashSet<>();

    public Usuario(
            final Long id,
            final String email,
            final String password,
            final String nombre,
            final Boolean activo,
            final LocalDateTime fechaCreacion,
            final LocalDateTime fechaActualizacion,
            final Rol rol
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nombre = nombre;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
        this.rol = rol;
        this.roles = new LinkedHashSet<>();
        sincronizarRoles();
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = LocalDateTime.now();
        }

        sincronizarRolPrincipal();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
        sincronizarRolPrincipal();
    }

    @PostLoad
    protected void onLoad() {
        sincronizarRoles();
    }

    public Set<Rol> getRoles() {
        sincronizarRoles();
        return roles;
    }

    public void setRoles(final Set<Rol> roles) {
        this.roles = roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
        sincronizarRolPrincipal();
    }

    public void setRol(final Rol rol) {
        this.rol = rol;
        if (roles == null) {
            roles = new LinkedHashSet<>();
        }
        if (rol != null && !roles.contains(rol)) {
            roles.clear();
            roles.add(rol);
        }
    }

    private void sincronizarRoles() {
        if (roles == null) {
            roles = new LinkedHashSet<>();
        }
        if (roles.isEmpty() && rol != null) {
            roles.add(rol);
        }
        sincronizarRolPrincipal();
    }

    private void sincronizarRolPrincipal() {
        if (roles != null && !roles.isEmpty()) {
            rol = roles.iterator().next();
        }
    }
}
