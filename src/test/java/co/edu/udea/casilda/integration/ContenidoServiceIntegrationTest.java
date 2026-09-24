package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.dto.request.ContenidoRequest;
import co.edu.udea.casilda.dto.response.ContenidoHomeResponse;
import co.edu.udea.casilda.dto.response.ContenidoResponse;
import co.edu.udea.casilda.model.entity.Contenido;
import co.edu.udea.casilda.model.enums.SeccionContenidoEnum;
import co.edu.udea.casilda.service.ContenidoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica las reglas de negocio de ContenidoService contra una base de datos
 * real: filtrado de vigencia, orden con desempate por título, borrado lógico
 * y validación de vigenciaFin.
 */
@Transactional
class ContenidoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ContenidoService contenidoService;

    @PersistenceContext
    private EntityManager entityManager;

    private void autenticarComo(final String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_GESTOR_CONTENIDO"))));
    }

    private Long persistirContenido(String titulo, Instant inicio, Instant fin, boolean eliminado, int orden) {
        Contenido contenido = new Contenido();
        contenido.setImagen(null);
        contenido.setTitulo(titulo);
        contenido.setContenido("Cuerpo de prueba para " + titulo);
        contenido.setVigenciaInicio(inicio);
        contenido.setVigenciaFin(fin);
        contenido.setEnlace(null);
        contenido.setSeccion(SeccionContenidoEnum.INFORMACION);
        contenido.setOrden(orden);
        contenido.setEliminado(eliminado);
        entityManager.persist(contenido);
        entityManager.flush();
        return contenido.getId();
    }

    @Test
    void listarVigentesHomeExcluyeFuturosExpiradosYEliminados() {
        autenticarComo("gestor@udea.edu.co");
        Instant ahora = Instant.now();

        persistirContenido("Vigente sin fin", ahora.minus(1, ChronoUnit.DAYS), null, false, 0);
        persistirContenido("Vigente con fin futuro", ahora.minus(1, ChronoUnit.DAYS),
                ahora.plus(1, ChronoUnit.DAYS), false, 0);
        persistirContenido("Futuro", ahora.plus(1, ChronoUnit.DAYS), null, false, 0);
        persistirContenido("Expirado", ahora.minus(2, ChronoUnit.DAYS), ahora.minus(1, ChronoUnit.DAYS), false, 0);
        persistirContenido("Eliminado", ahora.minus(1, ChronoUnit.DAYS), null, true, 0);

        List<ContenidoHomeResponse> vigentes = contenidoService.listarVigentesHome();

        assertThat(vigentes).extracting(ContenidoHomeResponse::getTitulo)
                .containsExactlyInAnyOrder("Vigente sin fin", "Vigente con fin futuro");
    }

    @Test
    void listarVigentesHomeOrdenaPorOrdenYDesempataPorTitulo() {
        autenticarComo("gestor@udea.edu.co");
        Instant ahora = Instant.now();

        persistirContenido("Zebra", ahora.minus(1, ChronoUnit.DAYS), null, false, 1);
        persistirContenido("Alfa", ahora.minus(1, ChronoUnit.DAYS), null, false, 1);
        persistirContenido("Primero", ahora.minus(1, ChronoUnit.DAYS), null, false, 0);

        List<ContenidoHomeResponse> vigentes = contenidoService.listarVigentesHome();

        assertThat(vigentes).extracting(ContenidoHomeResponse::getTitulo)
                .containsExactly("Primero", "Alfa", "Zebra");
    }

    @Test
    void listarVigentesHomeRetornaListaVaciaSinContenidos() {
        autenticarComo("gestor@udea.edu.co");

        List<ContenidoHomeResponse> vigentes = contenidoService.listarVigentesHome();

        assertThat(vigentes).isEmpty();
    }

    @Test
    void crearActualizarYEliminarLogicamenteContenido() {
        autenticarComo("gestor@udea.edu.co");

        ContenidoRequest request = new ContenidoRequest();
        request.setTitulo("Atención por Línea Alma");
        request.setContenido("Línea de escucha y apoyo psicológico inmediato.");
        request.setVigenciaInicio(Instant.parse("2026-01-01T00:00:00Z"));
        request.setVigenciaFin(null);
        request.setEnlace(null);
        request.setSeccion("acciones");
        request.setOrden(2);

        MultipartFile imagenInicial = new MockMultipartFile(
                "imagen", "linea-alma.png", "image/png", new byte[]{1, 2, 3, 4});

        ContenidoResponse creado = contenidoService.crear(request, imagenInicial);
        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getSeccion()).isEqualTo("acciones");
        assertThat(creado.getEliminado()).isFalse();
        assertThat(creado.getImagen()).startsWith("/contenidos/imagenes/").endsWith(".png");

        request.setTitulo("Atención por Línea Alma (actualizado)");
        ContenidoResponse actualizado = contenidoService.actualizar(creado.getId(), request, null, false);
        assertThat(actualizado.getTitulo()).isEqualTo("Atención por Línea Alma (actualizado)");
        assertThat(actualizado.getImagen())
                .as("La imagen se conserva cuando no se envía un archivo nuevo ni se pide eliminarla")
                .isEqualTo(creado.getImagen());

        ContenidoResponse sinImagen = contenidoService.actualizar(creado.getId(), request, null, true);
        assertThat(sinImagen.getImagen()).isNull();

        contenidoService.eliminar(creado.getId());

        Page<ContenidoResponse> pagina = contenidoService.listarPaginado(0, 10);
        assertThat(pagina.getContent()).extracting(ContenidoResponse::getId)
                .doesNotContain(creado.getId());

        assertThatThrownBy(() -> contenidoService.obtenerPorId(creado.getId()))
                .isInstanceOf(co.edu.udea.casilda.exception.ResourceNotFoundException.class);
    }

    @Test
    void rechazaVigenciaFinAnteriorOIgualAVigenciaInicio() {
        autenticarComo("gestor@udea.edu.co");

        ContenidoRequest request = new ContenidoRequest();
        request.setTitulo("Contenido inválido");
        request.setContenido("Cuerpo");
        request.setVigenciaInicio(Instant.parse("2026-01-01T00:00:00Z"));
        request.setVigenciaFin(Instant.parse("2026-01-01T00:00:00Z"));
        request.setSeccion("informacion");

        assertThatThrownBy(() -> contenidoService.crear(request, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
