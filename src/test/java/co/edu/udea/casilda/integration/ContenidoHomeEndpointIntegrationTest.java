package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.model.entity.Contenido;
import co.edu.udea.casilda.model.enums.SeccionContenidoEnum;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica, a través de una petición HTTP real a GET /contenidos/home, que
 * solo se devuelven los contenidos vigentes (excluyendo futuros, expirados y
 * eliminados lógicamente), sin necesidad de autenticación.
 */
@Transactional
class ContenidoHomeEndpointIntegrationTest extends IntegrationTestBase {

    @PersistenceContext
    private EntityManager entityManager;

    private void persistirContenido(String titulo, Instant inicio, Instant fin, boolean eliminado) {
        Contenido contenido = new Contenido();
        contenido.setImagen(null);
        contenido.setTitulo(titulo);
        contenido.setContenido("Cuerpo de prueba para " + titulo);
        contenido.setVigenciaInicio(inicio);
        contenido.setVigenciaFin(fin);
        contenido.setEnlace(null);
        contenido.setSeccion(SeccionContenidoEnum.INFORMACION);
        contenido.setOrden(0);
        contenido.setEliminado(eliminado);
        entityManager.persist(contenido);
        entityManager.flush();
    }

    @Test
    void homeSoloDevuelveContenidosVigentes() throws Exception {
        Instant ahora = Instant.now();

        persistirContenido("Vigente sin fin", ahora.minus(1, ChronoUnit.DAYS), null, false);
        persistirContenido("Vigente con fin futuro", ahora.minus(1, ChronoUnit.DAYS),
                ahora.plus(1, ChronoUnit.DAYS), false);
        persistirContenido("Futuro", ahora.plus(1, ChronoUnit.DAYS), null, false);
        persistirContenido("Expirado", ahora.minus(2, ChronoUnit.DAYS), ahora.minus(1, ChronoUnit.DAYS), false);
        persistirContenido("Eliminado", ahora.minus(1, ChronoUnit.DAYS), null, true);

        String body = mockMvc.perform(get("/contenidos/home"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        assertThat(json.isArray()).isTrue();

        var titulos = StreamSupport.stream(json.spliterator(), false)
                .map(nodo -> nodo.get("titulo").asText())
                .toList();

        assertThat(titulos).containsExactlyInAnyOrder("Vigente sin fin", "Vigente con fin futuro");
    }
}
