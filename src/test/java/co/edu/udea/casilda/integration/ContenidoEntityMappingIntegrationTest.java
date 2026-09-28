package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.model.entity.Contenido;
import co.edu.udea.casilda.model.enums.SeccionContenidoEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el mapeo JPA/BD de la entidad Contenido: persistencia de un cuerpo
 * extenso (equivalente a un artículo), imagen opcional, enumeración de sección
 * cerrada y los valores por defecto de orden/eliminado.
 */
@Transactional
class ContenidoEntityMappingIntegrationTest extends IntegrationTestBase {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void guardaYRecuperaContenidoConCuerpoExtensoYSinImagen() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gestor@udea.edu.co", "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        String cuerpoExtenso = "Artículo institucional. ".repeat(200); // > 500 caracteres

        Contenido contenido = new Contenido();
        contenido.setImagen(null);
        contenido.setTitulo("¿Quién es CASILDA?");
        contenido.setContenido(cuerpoExtenso);
        contenido.setVigenciaInicio(Instant.parse("2026-01-01T00:00:00Z"));
        contenido.setVigenciaFin(null);
        contenido.setEnlace(null);
        contenido.setSeccion(SeccionContenidoEnum.INFORMACION);

        entityManager.persist(contenido);
        entityManager.flush();
        entityManager.clear();

        Contenido recuperado = entityManager.find(Contenido.class, contenido.getId());

        assertThat(recuperado).isNotNull();
        assertThat(recuperado.getImagen()).isNull();
        assertThat(recuperado.getContenido()).isEqualTo(cuerpoExtenso);
        assertThat(recuperado.getContenido().length()).isGreaterThan(500);
        assertThat(recuperado.getSeccion()).isEqualTo(SeccionContenidoEnum.INFORMACION);
        assertThat(recuperado.getVigenciaFin()).isNull();
        assertThat(recuperado.getOrden()).isEqualTo(0);
        assertThat(recuperado.getEliminado()).isFalse();
        assertThat(recuperado.getCreatedAt()).isNotNull();
        assertThat(recuperado.getCreatedBy()).isEqualTo("gestor@udea.edu.co");
    }

    @Test
    void guardaContenidoConImagenYEnlaceInterno() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gestor@udea.edu.co", "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        Contenido contenido = new Contenido();
        contenido.setImagen("assets/uad_equipo_3_y_4.svg");
        contenido.setTitulo("Registrar queja disciplinaria (UAD 3 y 4)");
        contenido.setContenido("Registra formalmente una queja ante la Unidad de Asuntos Disciplinarios.");
        contenido.setVigenciaInicio(Instant.now().minus(1, ChronoUnit.DAYS));
        contenido.setVigenciaFin(null);
        contenido.setEnlace("/formulario-anonimo");
        contenido.setSeccion(SeccionContenidoEnum.ACCIONES);
        contenido.setOrden(1);

        entityManager.persist(contenido);
        entityManager.flush();
        entityManager.clear();

        Contenido recuperado = entityManager.find(Contenido.class, contenido.getId());

        assertThat(recuperado.getImagen()).isEqualTo("assets/uad_equipo_3_y_4.svg");
        assertThat(recuperado.getEnlace()).isEqualTo("/formulario-anonimo");
        assertThat(recuperado.getSeccion()).isEqualTo(SeccionContenidoEnum.ACCIONES);
        assertThat(recuperado.getOrden()).isEqualTo(1);
        assertThat(recuperado.getEliminado()).isFalse();
    }
}
