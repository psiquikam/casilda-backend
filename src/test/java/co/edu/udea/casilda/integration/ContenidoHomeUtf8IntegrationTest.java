package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.model.entity.Contenido;
import co.edu.udea.casilda.model.enums.SeccionContenidoEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el endpoint público del Home entregue correctamente los
 * caracteres en español (tildes y "ñ") en UTF-8, según exige el contrato de
 * "Contenido dinámico del Home".
 */
@Transactional
class ContenidoHomeUtf8IntegrationTest extends IntegrationTestBase {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void devuelveTextosConTildesYEneEnUtf8() throws Exception {
        Contenido contenido = new Contenido();
        contenido.setImagen("assets/distintivo_casilda_morado.svg");
        contenido.setTitulo("¿Quién es CASILDA?");
        contenido.setContenido(
                "Es el sistema de vigilancia en salud pública de la UdeA para el abordaje "
                        + "de las discriminaciones y violencias basadas en género.");
        contenido.setVigenciaInicio(Instant.now().minus(1, ChronoUnit.DAYS));
        contenido.setVigenciaFin(null);
        contenido.setEnlace(null);
        contenido.setSeccion(SeccionContenidoEnum.INFORMACION);
        contenido.setOrden(0);
        entityManager.persist(contenido);
        entityManager.flush();

        mockMvc.perform(get("/contenidos/home").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].titulo").value("¿Quién es CASILDA?"))
                .andExpect(jsonPath("$[0].contenido").value(
                        "Es el sistema de vigilancia en salud pública de la UdeA para el abordaje "
                                + "de las discriminaciones y violencias basadas en género."));
    }
}
