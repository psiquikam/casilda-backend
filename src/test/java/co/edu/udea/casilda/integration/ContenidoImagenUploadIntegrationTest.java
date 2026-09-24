package co.edu.udea.casilda.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el flujo completo de carga de imagen a través de HTTP real: crear
 * un contenido con una imagen adjunta, confirmar que queda disponible como
 * recurso público (sin autenticación) en la ruta devuelta, reemplazarla y
 * finalmente eliminarla explícitamente.
 */
@Transactional
class ContenidoImagenUploadIntegrationTest extends IntegrationTestBase {

    private static final String CONTENIDO_VALIDO = """
            {
              "titulo": "Contenido con imagen",
              "contenido": "Cuerpo de prueba",
              "vigenciaInicio": "2026-01-01T00:00:00Z",
              "seccion": "informacion"
            }
            """;

    private MockMultipartFile partesContenido() {
        return new MockMultipartFile(
                "contenido", "", MediaType.APPLICATION_JSON_VALUE, CONTENIDO_VALIDO.getBytes());
    }

    @Test
    void creaContenidoConImagenYQuedaDisponiblePublicamente() throws Exception {
        MockMultipartFile imagen = new MockMultipartFile(
                "imagen", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3, 4});

        String body = mockMvc.perform(multipart("/contenidos")
                        .file(partesContenido())
                        .file(imagen)
                        .header("Authorization", bearer("gestor@udea.edu.co")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        String rutaImagen = json.get("imagen").asText();
        assertThat(rutaImagen).startsWith("/contenidos/imagenes/").endsWith(".png");

        // El recurso estático se sirve sin autenticación.
        mockMvc.perform(get(rutaImagen))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    void reemplazaLaImagenAlActualizarConUnArchivoNuevo() throws Exception {
        MockMultipartFile imagenInicial = new MockMultipartFile(
                "imagen", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3, 4});
        String bodyCreacion = mockMvc.perform(multipart("/contenidos")
                        .file(partesContenido())
                        .file(imagenInicial)
                        .header("Authorization", bearer("gestor@udea.edu.co")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(bodyCreacion).get("id").asLong();
        String rutaInicial = objectMapper.readTree(bodyCreacion).get("imagen").asText();

        MockMultipartFile imagenNueva = new MockMultipartFile(
                "imagen", "logo-nuevo.webp", "image/webp", new byte[]{5, 6, 7, 8});
        String bodyActualizacion = mockMvc.perform(multipart(HttpMethod.PUT, "/contenidos/" + id)
                        .file(partesContenido())
                        .file(imagenNueva)
                        .header("Authorization", bearer("gestor@udea.edu.co")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String rutaNueva = objectMapper.readTree(bodyActualizacion).get("imagen").asText();

        assertThat(rutaNueva).isNotEqualTo(rutaInicial).endsWith(".webp");
        mockMvc.perform(get(rutaNueva)).andExpect(status().isOk());
        // La imagen anterior fue eliminada físicamente: ya no es servible.
        mockMvc.perform(get(rutaInicial)).andExpect(status().isNotFound());
    }

    @Test
    void eliminaLaImagenExplicitamenteSinReemplazo() throws Exception {
        MockMultipartFile imagenInicial = new MockMultipartFile(
                "imagen", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3, 4});
        String bodyCreacion = mockMvc.perform(multipart("/contenidos")
                        .file(partesContenido())
                        .file(imagenInicial)
                        .header("Authorization", bearer("gestor@udea.edu.co")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(bodyCreacion).get("id").asLong();

        String bodyActualizacion = mockMvc.perform(multipart(HttpMethod.PUT, "/contenidos/" + id)
                        .file(partesContenido())
                        .param("eliminarImagen", "true")
                        .header("Authorization", bearer("gestor@udea.edu.co")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(bodyActualizacion).get("imagen").isNull()).isTrue();
    }

    @Test
    void rechazaUnFormatoDeImagenNoSoportado() throws Exception {
        MockMultipartFile imagenInvalida = new MockMultipartFile(
                "imagen", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/contenidos")
                        .file(partesContenido())
                        .file(imagenInvalida)
                        .header("Authorization", bearer("gestor@udea.edu.co")))
                .andExpect(status().isBadRequest());
    }
}
