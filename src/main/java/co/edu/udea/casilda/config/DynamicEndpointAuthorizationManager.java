package co.edu.udea.casilda.config;

import co.edu.udea.casilda.model.entity.Endpoint;
import co.edu.udea.casilda.model.entity.EndpointRole;
import co.edu.udea.casilda.model.entity.Rol;
import co.edu.udea.casilda.repository.EndpointRepository;
import co.edu.udea.casilda.repository.EndpointRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DynamicEndpointAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final EndpointRepository endpointRepository;
    private final EndpointRoleRepository endpointRoleRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public AuthorizationDecision check(
            final Supplier<Authentication> authenticationSupplier,
            final RequestAuthorizationContext context
    ) {
        final Authentication authentication = authenticationSupplier.get();
        final boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        final String method = context.getRequest().getMethod();
        final String path = getRequestPath(context);
        final List<Endpoint> endpoints = endpointRepository.findByActivoTrue().stream()
                .filter(endpoint -> method.equalsIgnoreCase(endpoint.getHttpMethod())
                        || "ANY".equalsIgnoreCase(endpoint.getHttpMethod()))
                .toList();
        final List<Endpoint> matchingEndpoints = endpoints.stream()
                .filter(endpoint -> pathMatcher.match(endpoint.getPath(), path))
                .toList();

        if (matchingEndpoints.isEmpty()) {
            return new AuthorizationDecision(false);
        }

        if (matchingEndpoints.stream().anyMatch(endpoint -> Boolean.TRUE.equals(endpoint.getPublico()))) {
            return new AuthorizationDecision(true);
        }

        if (!authenticated) {
            return new AuthorizationDecision(false);
        }

        final Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        final boolean endpointAllowsUser = matchingEndpoints.stream()
                .map(Endpoint::getId)
                .map(endpointRoleRepository::findByEndpointId)
                .anyMatch(endpointRoles -> hasAllowedRole(endpointRoles, authorities));

        return new AuthorizationDecision(endpointAllowsUser);
    }

    private String getRequestPath(final RequestAuthorizationContext context) {
        final String requestUri = context.getRequest().getRequestURI();
        final String contextPath = context.getRequest().getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private boolean hasAllowedRole(
            final List<EndpointRole> endpointRoles,
            final Collection<? extends GrantedAuthority> authorities
    ) {
        return endpointRoles.stream()
                .map(EndpointRole::getRol)
                .filter(role -> Boolean.TRUE.equals(role.getActivo()))
                .map(this::getRoleAuthority)
                .anyMatch(requiredAuthority -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(requiredAuthority::equals));
    }

    private String getRoleAuthority(final Rol role) {
        final String roleValue = role.getCodigo() == null || role.getCodigo().isBlank()
                ? role.getNombre()
                : role.getCodigo();
        return ROLE_PREFIX + roleValue.toUpperCase();
    }
}
