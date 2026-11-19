package co.edu.udea.casilda.service.uad;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

/**
 * Ejecuta una operación con una sesión anónima, aunque la petición traiga un token válido.
 *
 * <p>Las entidades UAD extienden {@code Auditable}, que guarda el usuario de la sesión en
 * {@code created_by} y {@code modified_by}. Una queja anónima (NDA) no debe quedar asociada a nadie
 * (UAD-NOT-04, UAD-SEG-06): dentro de este bloque la auditoría registra {@code ANONYMOUS}, también en el
 * contador de consecutivos que se actualiza en la misma transacción.</p>
 *
 * <p>La transacción debe abrirse y confirmarse <strong>dentro</strong> del bloque, porque la auditoría se
 * resuelve al hacer flush. Al salir se restaura la sesión original.</p>
 */
public final class UadSinIdentidad {

    private UadSinIdentidad() {
    }

    public static <T> T ejecutar(final Supplier<T> operacion) {
        final SecurityContext original = SecurityContextHolder.getContext();
        final SecurityContext anonimo = SecurityContextHolder.createEmptyContext();
        anonimo.setAuthentication(new AnonymousAuthenticationToken("uad-nda", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        SecurityContextHolder.setContext(anonimo);
        try {
            return operacion.get();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }
}
