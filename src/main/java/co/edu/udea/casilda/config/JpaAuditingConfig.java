package co.edu.udea.casilda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import java.util.Optional;

@Configuration
@EnableJpaAuditing()
public class JpaAuditingConfig {

   @Bean
    public AuditorAware<String> auditorProvider() {
    return () -> Optional.of(switch (SecurityContextHolder.getContext().getAuthentication()) {
        case null -> "SYSTEM";
        case AnonymousAuthenticationToken ignored -> "ANONYMOUS";
        case Authentication auth when !auth.isAuthenticated() -> "ANONYMOUS";
        case Authentication auth -> auth.getName();
    });
} 
}
