package co.edu.udea.casilda.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitudAuthorizationIntegrationTest extends IntegrationTestBase {

    static Stream<Arguments> allowedSolicitudesEndpoints() {
        return Stream.of(
                Arguments.of("admin@udea.edu.co", "GET", "/solicitudes/acompanamiento/100"),
                Arguments.of("admin@udea.edu.co", "POST", "/solicitudes/acompanamiento"),
                Arguments.of("admin@udea.edu.co", "PUT", "/solicitudes/acompanamiento/100"),
                Arguments.of("admin@udea.edu.co", "DELETE", "/solicitudes/acompanamiento/100"),
                Arguments.of("coordinador@udea.edu.co", "GET", "/solicitudes/acompanamiento/100"),
                Arguments.of("coordinador@udea.edu.co", "POST", "/solicitudes/acompanamiento"),
                Arguments.of("coordinador@udea.edu.co", "PUT", "/solicitudes/acompanamiento/100"),
                Arguments.of("coordinador@udea.edu.co", "DELETE", "/solicitudes/acompanamiento/100"),
                Arguments.of("user@udea.edu.co", "GET", "/solicitudes/acompanamiento/100"),
                Arguments.of("user@udea.edu.co", "POST", "/solicitudes/acompanamiento")
        );
    }

    @ParameterizedTest
    @MethodSource("allowedSolicitudesEndpoints")
    void rolesWithSolicitudPermissionPassAuthorization(
            String email, String method, String path) throws Exception {
        mockMvc.perform(request(method, path)
                        .header("Authorization", bearer(email))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus()));
    }

    static Stream<String> rolesWithoutSolicitudPermission() {
        return Stream.of("profesional@udea.edu.co", "revisor@udea.edu.co");
    }

    @ParameterizedTest
    @MethodSource("rolesWithoutSolicitudPermission")
    void rolesWithoutSolicitudPermissionAreForbidden(String email) throws Exception {
        mockMvc.perform(get("/solicitudes/acompanamiento/100")
                        .header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/solicitudes/acompanamiento/100")
                        .header("Authorization", bearer(email)))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.api.Test
    void anonymousSolicitudRequestIsForbidden() throws Exception {
        mockMvc.perform(get("/solicitudes/acompanamiento/100"))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(
            String method, String path) {
        return switch (method) {
            case "GET" -> get(path);
            case "POST" -> post(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        };
    }
}
