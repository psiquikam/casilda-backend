package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistroUsuarioIntegrationTest extends IntegrationTestBase {

    private static final String PASSWORD = "ContrasenaSegura123";

    @Autowired
    private JdbcTemplate jdbc;

    // El seed inserta ids fijos; las secuencias deben avanzar para que los inserts de la app no choquen.
    @BeforeEach
    void sincronizarSecuencias() {
        for (String tabla : new String[] {"usuario", "persona", "correopersona", "telefonopersona"}) {
            jdbc.queryForObject("select setval(pg_get_serial_sequence('" + tabla + "', 'id'), "
                    + "(select coalesce(max(id), 1) from " + tabla + "))", Long.class);
        }
    }

    private String body(final String documento, final String email, final boolean habeasData) {
        return """
                {"primerNombre":"Maria","segundoNombre":"Jose","primerApellido":"Rios","segundoApellido":"Gil",
                 "tipoDocumento":"CC","documento":"%s","vinculoUdeaId":1,"email":"%s",
                 "telefono":"3001234567","password":"%s","habeasData":%s}
                """.formatted(documento, email, PASSWORD, habeasData);
    }

    @Test
    void registraUsuarioNuevoConPersonaRolYLogin() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content(body("900000001", "Nuevo.Usuario@Example.com", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nuevo.usuario@example.com"));

        assertEquals(1, jdbc.queryForObject(
                "select count(*) from persona where numerodocumento = '900000001' and idvinculoudea = 1",
                Integer.class));
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from usuario u
                join usuariorol ur on ur.idusuario = u.id
                join rol r on r.id = ur.idrol
                where u.email = 'nuevo.usuario@example.com' and r.codigo = 'USUARIO'
                  and u.habeasdata = true and u.fechahabeasdata is not null and u.idpersona is not null
                """, Integer.class));
        String hash = jdbc.queryForObject(
                "select password from usuario where email = 'nuevo.usuario@example.com'", String.class);
        assertNotEquals(PASSWORD, hash);
        assertTrue(hash.startsWith("$2"));

        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"nuevo.usuario@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("USUARIO")));
    }

    @Test
    void correoDuplicadoRetorna409() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                .content(body("900000002", "dup@example.com", true))).andExpect(status().isCreated());
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                .content(body("900000003", "DUP@example.com", true))).andExpect(status().isConflict());
    }

    @Test
    void documentoYaVinculadoAOtroUsuarioRetorna409() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                .content(body("900000004", "uno@example.com", true))).andExpect(status().isCreated());
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                .content(body("900000004", "dos@example.com", true))).andExpect(status().isConflict());
    }

    @Test
    void reutilizaPersonaExistenteSinModificarla() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content("""
                                {"primerNombre":"Otro","primerApellido":"Nombre","segundoApellido":"Distinto",
                                 "tipoDocumento":"CC","documento":"100000001","email":"ana.reuso@example.com",
                                 "telefono":"3001234567","password":"%s","habeasData":true}
                                """.formatted(PASSWORD)))
                .andExpect(status().isCreated());

        assertEquals("Ana", jdbc.queryForObject(
                "select primernombre from persona where numerodocumento = '100000001'", String.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from persona where numerodocumento = '100000001'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from usuario u join persona p on p.id = u.idpersona "
                        + "where p.numerodocumento = '100000001'", Integer.class));
    }

    @Test
    void registraSinSegundoApellido() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                        .content("""
                                {"primerNombre":"Sofia","primerApellido":"Rios",
                                 "tipoDocumento":"CC","documento":"900000007","vinculoUdeaId":1,
                                 "email":"sin.segundo@example.com","telefono":"3001234567",
                                 "password":"%s","habeasData":true}
                                """.formatted(PASSWORD)))
                .andExpect(status().isCreated());

        assertEquals(1, jdbc.queryForObject(
                "select count(*) from persona where numerodocumento = '900000007' and segundoapellido is null",
                Integer.class));
        assertEquals("Sofia Rios", jdbc.queryForObject(
                "select nombre from usuario where email = 'sin.segundo@example.com'", String.class));
    }

    @Test
    void habeasDataFalsoRetorna400() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                .content(body("900000005", "sin.habeas@example.com", false))).andExpect(status().isBadRequest());
    }

    @Test
    void tipoDocumentoInvalidoRetorna404() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON)
                .content(body("900000006", "tipo@example.com", true).replace("\"CC\"", "\"XX\"")))
                .andExpect(status().isNotFound());
    }

    @Test
    void catalogoVinculosEsPublico() throws Exception {
        mockMvc.perform(get("/auth/vinculos-udea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
