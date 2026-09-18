package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.model.entity.Sexo;
import co.edu.udea.casilda.repository.SexoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que los campos heredados de Auditable se completen automáticamente
 * a través del ciclo de vida de JPA (creación y actualización de entidades).
 */
class JpaAuditingIntegrationTest extends IntegrationTestBase {

    private static final Integer SEXO_ID = 999;

    @Autowired
    private SexoRepository sexoRepository;

    @AfterEach
    void cleanUp() {
        sexoRepository.deleteById(SEXO_ID);
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesCreatedAtAndCreatedByOnInsert() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("auditor@udea.edu.co", "credentials", authorities));

        Sexo sexo = new Sexo();
        sexo.setId(SEXO_ID);
        sexo.setCodigo("AUD");
        sexo.setNombre("Auditoria Test");

        Sexo saved = sexoRepository.saveAndFlush(sexo);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("auditor@udea.edu.co");
        assertThat(saved.getModifiedAt()).isNotNull();
        assertThat(saved.getModifiedBy()).isEqualTo("auditor@udea.edu.co");
    }

    @Test
    void updatesModifiedAtAndModifiedByOnUpdate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("creador@udea.edu.co", "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        Sexo sexo = new Sexo();
        sexo.setId(SEXO_ID);
        sexo.setCodigo("AUD");
        sexo.setNombre("Auditoria Test");
        Sexo created = sexoRepository.saveAndFlush(sexo);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("editor@udea.edu.co", "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        created.setNombre("Auditoria Test Actualizada");
        Sexo updated = sexoRepository.saveAndFlush(created);

        assertThat(updated.getCreatedBy()).isEqualTo("creador@udea.edu.co");
        assertThat(updated.getModifiedBy()).isEqualTo("editor@udea.edu.co");
        assertThat(updated.getModifiedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }
}
