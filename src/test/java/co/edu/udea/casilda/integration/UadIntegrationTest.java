package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.model.entity.uad.UadEvento;
import co.edu.udea.casilda.repository.uad.UadEventoRepository;
import co.edu.udea.casilda.repository.uad.UadNoticiaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización y reglas UAD de punta a punta: rutas registradas en {@code endpoint}, roles reales en el JWT,
 * visibilidad por equipo en el servidor y radicación NDA/NDI. Requiere Docker (Testcontainers).
 */
// Un @Sql en la subclase reemplaza al de IntegrationTestBase (no se heredan ni se combinan): se declaran ambas semillas.
@Sql(scripts = {"/sql/integration-seed.sql", "/sql/uad-integration-seed.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UadIntegrationTest extends IntegrationTestBase {

    private static final String E3 = "uad.e3@udea.edu.co";
    private static final String E4 = "uad.e4@udea.edu.co";

    @Autowired
    private UadNoticiaRepository noticias;

    @Autowired
    private UadEventoRepository eventos;

    private String radicarNda(final String authorization) throws Exception {
        var peticion = post("/uad/noticias-anonimas").contentType(APPLICATION_JSON)
                .content("{\"descripcionHechos\":\"Relato ficticio de prueba con detalle suficiente.\",\"vinculacionDeclarada\":\"DOCENTE_CATEDRA\"}");
        if (authorization != null) peticion = peticion.header("Authorization", authorization);
        final String cuerpo = mockMvc.perform(peticion)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consecutivo", matchesPattern("NDA-\\d{4}-\\d{6}")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo).get("consecutivo").asText();
    }

    private List<String> consecutivos(final String email, final String tipo) throws Exception {
        final String cuerpo = mockMvc.perform(get("/uad/noticias").param("tipo", tipo).param("size", "50").header("Authorization", bearer(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        final List<String> lista = new java.util.ArrayList<>();
        objectMapper.readTree(cuerpo).get("content").forEach(n -> lista.add(n.get("consecutivo").asText()));
        return lista;
    }

    private String radicarNdiDocente() throws Exception {
        final String cuerpo = mockMvc.perform(post("/reportes").contentType(APPLICATION_JSON).content("""
                        {
                          "tipoDocumentoId": 1,
                          "numeroDocumento": "1020304050",
                          "fechaNacimiento": "1999-03-14",
                          "tipoLugar": "interno",
                          "campusId": 1,
                          "descripcionHechos": "Relato ficticio dirigido a la UAD.",
                          "rutaActivacionId": 2,
                          "presuntoAgresor": { "primerNombre": "Persona", "primerApellido": "Ficticia", "vinculoUniversidadId": 6 }
                        }
                        """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        final Long reporteId = objectMapper.readTree(cuerpo).get("id").asLong();
        return noticias.findByReporteCasoId(reporteId).orElseThrow().getConsecutivo();
    }

    @Test
    void ndaPublicaSinSesionYVisibleAAmbosEquipos() throws Exception {
        final String consecutivo = radicarNda(null);
        assertTrue(consecutivos(E3, "NDA").contains(consecutivo));
        assertTrue(consecutivos(E4, "NDA").contains(consecutivo));
    }

    @Test
    void ndaConSesionAbiertaNoAsociaIdentidad() throws Exception {
        final String consecutivo = radicarNda(bearer("user@udea.edu.co"));
        final var noticia = noticias.findAll().stream().filter(n -> n.getConsecutivo().equals(consecutivo)).findFirst().orElseThrow();
        assertNull(noticia.getReporteCaso());
        final List<UadEvento> historial = eventos.findByNoticiaIdOrderByFechaAscIdAsc(noticia.getId());
        assertEquals(1, historial.size());
        assertNull(historial.get(0).getProfesional());
    }

    @Test
    void reporteConRutaUadCreaNdiVisibleSoloAlEquipoCompetente() throws Exception {
        final String consecutivo = radicarNdiDocente();
        assertTrue(consecutivo.matches("NDI-\\d{4}-\\d{6}"));
        assertTrue(consecutivos(E3, "NDI").contains(consecutivo));
        assertFalse(consecutivos(E4, "NDI").contains(consecutivo));
    }

    @Test
    void otroEquipoRecibe404YLaDecisionUsaVersion() throws Exception {
        final String consecutivo = radicarNdiDocente();
        final UUID id = noticias.findAll().stream().filter(n -> n.getConsecutivo().equals(consecutivo)).findFirst().orElseThrow().getId();

        mockMvc.perform(get("/uad/noticias/" + id).header("Authorization", bearer(E4))).andExpect(status().isNotFound());

        final JsonNode detalle = objectMapper.readTree(mockMvc.perform(get("/uad/noticias/" + id).header("Authorization", bearer(E3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casoId").doesNotExist())
                .andReturn().getResponse().getContentAsString());
        final long version = detalle.get("version").asLong();
        final String decision = """
                {"tipo":"CONTINUAR_TRAMITE","version":%d,"continuarTramite":{"modoCaso":"CREAR_NUEVO"}}""".formatted(version);

        mockMvc.perform(post("/uad/noticias/" + id + "/decisiones").header("Authorization", bearer(E3)).contentType(APPLICATION_JSON).content(decision))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noticia.estado").value("PROCEDE_TRAMITE"))
                .andExpect(jsonPath("$.noticia.casoId", matchesPattern("CAS-[A-Z2-9]{10}")))
                .andExpect(jsonPath("$.casoUad.equipoResponsable").value("EQUIPO_3"));

        mockMvc.perform(post("/uad/noticias/" + id + "/decisiones").header("Authorization", bearer(E3)).contentType(APPLICATION_JSON).content(decision))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOTICIA_YA_DECIDIDA"));
    }

    @Test
    void autorizacionPorRol() throws Exception {
        // Profesional del Equipo de Atención: la ruta no lo autoriza.
        mockMvc.perform(get("/uad/contexto").header("Authorization", bearer("profesional@udea.edu.co"))).andExpect(status().isForbidden());
        // ADMIN ve el contexto vacío, pero no las bandejas (catálogo de roles §1, D-UAD-03).
        mockMvc.perform(get("/uad/contexto").header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.equipos").isEmpty());
        mockMvc.perform(get("/uad/noticias").param("tipo", "NDA").header("Authorization", bearer("admin@udea.edu.co"))).andExpect(status().isForbidden());
        // Coordinación sin matriz aprobada: contexto sin capacidades.
        mockMvc.perform(get("/uad/contexto").header("Authorization", bearer("uad.coord@udea.edu.co")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.coordinacion").value(true)).andExpect(jsonPath("$.capacidades").isEmpty());
        // Sin sesión: denegado.
        mockMvc.perform(get("/uad/noticias").param("tipo", "NDA")).andExpect(status().isForbidden());
    }
}
