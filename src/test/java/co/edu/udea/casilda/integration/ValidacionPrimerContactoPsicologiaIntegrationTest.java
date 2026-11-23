package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.service.ValidacionPrimerContactoPsicologia;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ValidacionPrimerContactoPsicologiaIntegrationTest extends IntegrationTestBase {
    private static final Instant AHORA = Instant.parse("2026-11-22T12:00:00Z");
    @Autowired private ValidacionPrimerContactoPsicologia validacion;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    private ObjectNode request;
    private ObjectNode atencion;

    @BeforeEach
    void preparar() throws Exception {
        new ResourceDatabasePopulator(new FileSystemResource(
                "database/insert_catalogos_primer_contacto_psicologia.sql")).execute(dataSource);
        jdbc.update("""
                INSERT INTO catalogoalmaitem (catalogo,codigo,etiqueta,activo)
                VALUES ('textos-consentimiento','test-primer-contacto','Texto de prueba',true)
                ON CONFLICT (catalogo,codigo) DO UPDATE SET activo=true, etiqueta=EXCLUDED.etiqueta
                """);
        atencion = (ObjectNode) objectMapper.readTree("""
                {"canalIngreso":"LLAMADA","persona":{"tipoIdentificacion":"CC",
                "numeroIdentificacion":"1234567890","primerNombre":"Nombre APH"}}
                """);
        request = (ObjectNode) objectMapper.readTree("""
                {"version":4,"consentimiento":{"otorgado":true,"medio":"VERBAL",
                 "fechaRegistro":"2026-11-22T11:00:00Z","versionTexto":"test-primer-contacto"},
                 "persona":{"primerNombre":"María del Carmen","primerApellido":"De la Cruz"},
                 "valoracionRiesgo":{"nivel":"MEDIO","descripcion":"Descripción de riesgo"},
                 "dimensiones":["DIM_01"],"presuntoAgresor":null,
                 "acuerdos":"Acuerdos de atención","proximoControl":"2026-11-23T12:00:00Z"}
                """);
    }

    @Test
    void validaDatosYConservaDocumentoSinMutarAtencionOriginal() {
        var resultado = validacion.validar(request, atencion, AHORA);
        assertEquals("María del Carmen", resultado.persona().path("primerNombre").asText());
        assertEquals("De la Cruz", resultado.persona().path("primerApellido").asText());
        assertEquals("1234567890", resultado.persona().path("numeroIdentificacion").asText());
        assertEquals("Nombre APH", atencion.path("persona").path("primerNombre").asText());
        assertEquals(Instant.parse("2026-11-23T12:00:00Z"), resultado.proximoControl());
    }

    @Test
    void permiteNombresOAgresorAusentesYCanalDeRemision() {
        request.remove("persona");
        request.remove("dimensiones");
        atencion.put("canalIngreso", "REMISION_EA");
        ((ObjectNode) atencion.get("persona")).remove("primerNombre");
        var resultado = validacion.validar(request, atencion, AHORA);
        assertFalse(resultado.persona().has("primerNombre"));
        assertNull(resultado.datos().get("dimensiones"));
        assertNotNull(resultado.proximoControl());
    }

    @Test
    void rechazaErroresClinicosYCatalogosInvalidos() throws Exception {
        request.set("valoracionRiesgo", objectMapper.readTree("{\"nivel\":\"ALTA\",\"descripcion\":\"corta\"}"));
        request.put("acuerdos", "corto");
        request.put("proximoControl", "2026-11-22T12:00:00Z");
        request.set("dimensiones", objectMapper.readTree("[\"NO_EXISTE\"]"));
        request.set("presuntoAgresor", objectMapper.readTree("{\"vinculo\":\"NO_EXISTE\"}"));
        var ex = assertThrows(AtencionAlmaException.class, () -> validacion.validar(request, atencion, AHORA));
        assertEquals("VALIDACION", ex.getCodigo());
        for (String campo : new String[]{"valoracionRiesgo.nivel", "valoracionRiesgo.descripcion",
                "acuerdos", "proximoControl", "dimensiones", "presuntoAgresor.vinculo"}) {
            assertTrue(ex.getDetalles().containsKey(campo), campo);
        }
    }

    @Test
    void rechazaConsentimientoAusente() {
        request.remove("consentimiento");
        var sinConsentimiento = assertThrows(AtencionAlmaException.class,
                () -> validacion.validar(request, atencion, AHORA));
        assertEquals("CONSENTIMIENTO_REQUERIDO", sinConsentimiento.getCodigo());
    }

    @Test
    void rechazaCambioDeDocumentoYTiposInvalidos() {
        ((ObjectNode) request.get("persona")).put("numeroIdentificacion", "1111111111");
        var identidad = assertThrows(AtencionAlmaException.class, () -> validacion.validar(request, atencion, AHORA));
        assertTrue(identidad.getDetalles().containsKey("persona.numeroIdentificacion"));
        ((ObjectNode) request.get("persona")).remove("numeroIdentificacion");
        ((ObjectNode) request.get("persona")).put("estrato", 9);
        var estrato = assertThrows(AtencionAlmaException.class, () -> validacion.validar(request, atencion, AHORA));
        assertTrue(estrato.getDetalles().containsKey("persona.estrato"));
    }

    @Test
    void rechazaCatalogosInactivosYFechasSinZona() throws Exception {
        jdbc.update("""
                UPDATE catalogoalmaitem SET activo=false
                WHERE catalogo='dimensiones-salud-mental' AND codigo='DIM_01'
                """);
        request.put("proximoControl", "2026-11-23T12:00:00");
        request.set("presuntoAgresor", objectMapper.readTree("{\"vinculo\":\"EXPAREJA\",\"convivencia\":false}"));
        var ex = assertThrows(AtencionAlmaException.class, () -> validacion.validar(request, atencion, AHORA));
        assertTrue(ex.getDetalles().containsKey("dimensiones"));
        assertTrue(ex.getDetalles().containsKey("proximoControl"));
        assertFalse(ex.getDetalles().containsKey("presuntoAgresor.vinculo"));
    }

    @Test
    void validaLosLimitesDeTextoClinico() {
        ((ObjectNode) request.get("valoracionRiesgo")).put("descripcion", "a".repeat(10));
        request.put("acuerdos", "b".repeat(2000));
        validacion.validar(request, atencion, AHORA);

        request.put("acuerdos", "b".repeat(2001));
        var ex = assertThrows(AtencionAlmaException.class, () -> validacion.validar(request, atencion, AHORA));
        assertTrue(ex.getDetalles().containsKey("acuerdos"));
    }

    @Test
    void rechazaConsentimientoConVersionInactiva() {
        jdbc.update("""
                UPDATE catalogoalmaitem SET activo=false
                WHERE catalogo='textos-consentimiento' AND codigo='test-primer-contacto'
                """);
        var ex = assertThrows(AtencionAlmaException.class, () -> validacion.validar(request, atencion, AHORA));
        assertTrue(ex.getDetalles().containsKey("consentimiento.versionTexto"));
    }
}
