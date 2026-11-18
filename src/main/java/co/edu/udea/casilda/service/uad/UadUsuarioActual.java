package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.exception.UadException;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Equipos UAD del usuario autenticado, leídos de sus authorities ({@code ROLE_UAD_EQUIPO_3},
 * {@code ROLE_UAD_EQUIPO_4}, {@code ROLE_UAD_COORDINACION}). Nunca se aceptan desde el cliente.
 *
 * <p>ADMIN sin rol de equipo <strong>no</strong> obtiene equipo (catálogo de roles §1: la jerarquía no
 * da acceso a expedientes reservados). Coordinación sin matriz aprobada: sin lectura (D-UAD-03).</p>
 */
@Component
@RequiredArgsConstructor
public class UadUsuarioActual {

    public static final String ROL_EQUIPO_3 = "ROLE_UAD_EQUIPO_3";
    public static final String ROL_EQUIPO_4 = "ROLE_UAD_EQUIPO_4";
    public static final String ROL_COORDINACION = "ROLE_UAD_COORDINACION";

    private final UsuarioRepository usuarioRepository;

    public record Actor(String email, String nombre, List<EquipoUad> equipos, boolean coordinacion) {

        /** Equipo con el que se firman los eventos (el primero, si tuviera ambos). */
        public EquipoUad equipoPrincipal() {
            return equipos.isEmpty() ? null : equipos.get(0);
        }
    }

    public Actor actual() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UadException(HttpStatus.UNAUTHORIZED, "SIN_SESION", "Inicie sesión para continuar.");
        }
        final Set<String> autoridades = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        final List<EquipoUad> equipos = new ArrayList<>();
        if (autoridades.contains(ROL_EQUIPO_3)) equipos.add(EquipoUad.EQUIPO_3);
        if (autoridades.contains(ROL_EQUIPO_4)) equipos.add(EquipoUad.EQUIPO_4);
        final String nombre = usuarioRepository.findByEmailIgnoreCase(auth.getName()).map(Usuario::getNombre).orElse(auth.getName());
        return new Actor(auth.getName(), nombre, List.copyOf(equipos), autoridades.contains(ROL_COORDINACION));
    }

    /** Actor con al menos un equipo; si no, 403 {@code SIN_EQUIPO_UAD}. */
    public Actor conEquipo() {
        final Actor actor = actual();
        if (actor.equipos().isEmpty()) {
            throw new UadException(HttpStatus.FORBIDDEN, "SIN_EQUIPO_UAD", "Su usuario no tiene un equipo UAD asignado.");
        }
        return actor;
    }
}
