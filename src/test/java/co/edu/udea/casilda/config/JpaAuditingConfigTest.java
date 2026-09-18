package co.edu.udea.casilda.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingConfigTest {

    private final AuditorAware<String> auditorProvider = new JpaAuditingConfig().auditorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSystemWhenNoAuthenticationPresent() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorProvider.getCurrentAuditor();

        assertThat(auditor).contains("SYSTEM");
    }

    @Test
    void returnsAnonymousForAnonymousAuthenticationToken() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
        AnonymousAuthenticationToken anonymousToken =
                new AnonymousAuthenticationToken("key", "anonymousUser", authorities);
        SecurityContextHolder.getContext().setAuthentication(anonymousToken);

        Optional<String> auditor = auditorProvider.getCurrentAuditor();

        assertThat(auditor).contains("ANONYMOUS");
    }

    @Test
    void returnsAnonymousWhenAuthenticationIsNotAuthenticated() {
        UsernamePasswordAuthenticationToken unauthenticatedToken =
                new UsernamePasswordAuthenticationToken("someUser", "credentials");
        unauthenticatedToken.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauthenticatedToken);

        Optional<String> auditor = auditorProvider.getCurrentAuditor();

        assertThat(auditor).contains("ANONYMOUS");
    }

    @Test
    void returnsAuthenticatedUsername() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken authenticatedToken =
                new UsernamePasswordAuthenticationToken("usuario@udea.edu.co", "credentials", authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticatedToken);

        Optional<String> auditor = auditorProvider.getCurrentAuditor();

        assertThat(auditor).contains("usuario@udea.edu.co");
    }
}
