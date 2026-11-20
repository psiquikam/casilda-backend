package co.edu.udea.casilda.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * Alcance de lectura sobre las solicitudes de acompañamiento (D-UAD-16).
 *
 * <p>La tabla {@code endpoint} da {@code GET /solicitudes/**} tanto al equipo de atención como al rol
 * {@code USUARIO}. La ruta sola no distingue de quién es cada solicitud, así que el servicio limita a
 * quien no es del equipo a las solicitudes que esa misma persona radicó.</p>
 */
public final class SolicitudAcceso {

    /** Roles que consultan las solicitudes de todas las personas (ver {@code database/initial_data.sql}). */
    static final Set<String> ROLES_EQUIPO_ATENCION = Set.of("ROLE_ADMIN", "ROLE_COORDINADOR", "ROLE_PROFESIONAL", "ROLE_REVISOR");

    private SolicitudAcceso() {
    }

    /** {@code true} si la sesión actual solo puede ver las solicitudes que ella misma radicó. */
    public static boolean soloPropias() {
        return soloPropias(SecurityContextHolder.getContext().getAuthentication());
    }

    /** Sin sesión o sin rol del equipo: solo lo propio. Ante la duda se restringe. */
    static boolean soloPropias(final Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return true;
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).noneMatch(ROLES_EQUIPO_ATENCION::contains);
    }
}
