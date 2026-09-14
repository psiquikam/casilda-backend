package co.edu.udea.casilda.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicEndpointAuthorizationIntegrationTest extends IntegrationTestBase {

    static Stream<Arguments> allowedEndpointGroups() {
        return Stream.of(
                Arguments.of("admin@udea.edu.co", "/usuarios"),
                Arguments.of("admin@udea.edu.co", "/casos/paginado"),
                Arguments.of("coordinador@udea.edu.co", "/solicitudes/acompanamiento"),
                Arguments.of("profesional@udea.edu.co", "/atenciones/pestana/5"),
                Arguments.of("revisor@udea.edu.co", "/citas"),
                Arguments.of("user@udea.edu.co", "/personas/documento/does-not-exist"),
                Arguments.of("user@udea.edu.co", "/maestros/paises")
        );
    }

    static Stream<Arguments> deniedEndpointGroups() {
        return Stream.of(
                Arguments.of("user@udea.edu.co", "/usuarios"),
                Arguments.of("user@udea.edu.co", "/casos/paginado"),
                Arguments.of("profesional@udea.edu.co", "/solicitudes/acompanamiento"),
                Arguments.of("revisor@udea.edu.co", "/parametros/max-llamadas-contacto"),
                Arguments.of("coordinador@udea.edu.co", "/usuarios")
        );
    }

    static Stream<Arguments> allowedWriteGroups() {
        return Stream.of(
                Arguments.of("admin@udea.edu.co", "POST", "/casos/pestana/0"),
                Arguments.of("profesional@udea.edu.co", "POST", "/atenciones/pestana/5"),
                Arguments.of("revisor@udea.edu.co", "PUT", "/citas/1/reprogramar"),
                Arguments.of("coordinador@udea.edu.co", "POST", "/compromisos/persona"),
                Arguments.of("admin@udea.edu.co", "POST", "/linea-alma/registros/pestana/0"),
                Arguments.of("coordinador@udea.edu.co", "POST", "/solicitudes/acompanamiento"),
                Arguments.of("admin@udea.edu.co", "DELETE", "/solicitudes/acompanamiento/100")
        );
    }

    @ParameterizedTest(name = "{0} may reach {1} (business response is acceptable)")
    @MethodSource("allowedEndpointGroups")
    void grantedRolePassesAuthorization(String email, String path) throws Exception {
        int response = mockMvc.perform(get(path).header("Authorization", bearer(email)))
                .andReturn().getResponse().getStatus();
        assertNotEquals(403, response);
    }

    @ParameterizedTest(name = "{0} is denied from {1}")
    @MethodSource("deniedEndpointGroups")
    void missingRoleIsForbidden(String email, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} may use {1} {2}")
    @MethodSource("allowedWriteGroups")
    void grantedWriteRolePassesAuthorization(String email, String method, String path) throws Exception {
        var request = switch (method) {
            case "POST" -> post(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException(method);
        };
        int response = mockMvc.perform(request.header("Authorization", bearer(email))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(403, response);
    }

    @ParameterizedTest
    @MethodSource("protectedPaths")
    void anonymousRequestsAreForbidden(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isForbidden());
    }

    static Stream<String> protectedPaths() {
        return Stream.of("/usuarios", "/casos/paginado", "/solicitudes/acompanamiento",
                "/atenciones/pestana/5", "/citas", "/compromisos/persona/1",
                "/linea-alma/registros", "/personas/documento/1", "/parametros/max-llamadas-contacto",
                "/maestros/paises");
    }
}
