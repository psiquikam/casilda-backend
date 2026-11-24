package co.edu.udea.casilda.integration;

import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.alma.consulta-previa.habilitada=true")
class ConsultaPreviaAlmaIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @BeforeEach
    void preparar() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('endpoint', 'id'), (SELECT MAX(id) FROM endpoint))");
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new FileSystemResource("database/update_add_rol_tecnico_aph.sql"),
                new FileSystemResource("database/update_auditoria_recursos.sql"),
                new FileSystemResource("database/update_consulta_previa_aph.sql"),
                new FileSystemResource("database/migration_primer_contacto_psicologia.sql"));
        scripts.execute(dataSource);
        scripts.execute(dataSource);
        jdbc.update("""
                INSERT INTO usuario (id,email,password,nombre,activo,fechacreacion,fechaactualizacion,idrol)
                SELECT 8701,'consulta.aph@example.edu.co',u.password,'Técnico de prueba',true,CURRENT_TIMESTAMP,
                       CURRENT_TIMESTAMP,r.id FROM usuario u JOIN rol r ON r.codigo='TECNICO_APH' WHERE u.id=1
                """);
        jdbc.update("INSERT INTO usuariorol (idusuario,idrol) SELECT 8701,id FROM rol WHERE codigo='TECNICO_APH'");
        jdbc.update("INSERT INTO estadocaso (id,nombre) VALUES (8701,'Abierto activo'),(8702,'Cerrado')");
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (8701,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (8701,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (8701,'Prueba')");
        jdbc.update("INSERT INTO estadoatencion (id,nombre) VALUES (8701,'Abierto activo')");
        jdbc.update("INSERT INTO tiposeguimiento (id,nombre) VALUES (8701,'Prueba')");
        jdbc.update("INSERT INTO accion (id,nombre) VALUES (8701,'Seguimiento por psicología')");
        jdbc.update("INSERT INTO actividad (id,idaccion,nombre) VALUES (8701,8701,'Prueba')");
        jdbc.update("INSERT INTO estadoseguimiento (id,nombre) VALUES (8701,'En Proceso'),(8702,'Pendiente'),(8703,'Cerrado')");
        jdbc.update("INSERT INTO motivoestadoseguimiento (id,nombre) VALUES (8701,'Prueba')");
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (8701,100,CURRENT_TIMESTAMP,8701)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps,idestadocaso,fechacreacion,fechacierre)
                VALUES (8701,'CAS-ABIERTO',8701,1,8701,8701,8701,'2024-01-01',NULL),
                       (8702,'CAS-CON-FECHA',8701,1,8701,8701,8702,'2023-01-01','2024-01-01T00:00:00Z'),
                       (8703,'CAS-SIN-FECHA',8701,1,8701,8701,8702,'2025-01-01',NULL)
                """);
        jdbc.update("INSERT INTO atencion (id,idcaso,idestadoatencion,logroacuerdo) VALUES (8701,8701,8701,false)");
        jdbc.update("""
                INSERT INTO seguimientoatencion (id,idatencion,idtiposeguimiento,fecha,idaccion,idactividad,
                    descripcion,idestadoseguimiento,idmotivoestado)
                VALUES (8701,8701,8701,localtimestamp - interval '1 day',8701,8701,'privado',8701,8701),
                       (8702,8701,8701,localtimestamp + interval '2 days',8701,8701,'privado',8702,8701),
                       (8703,8701,8701,localtimestamp + interval '3 days',8701,8701,'privado',8701,8701)
                """);
        jdbc.update("""
                INSERT INTO asignacion (id,idsolicitudatencion,idgrupoprofesional,idtipoasignacion,idtiposervicio,fechacreacion)
                VALUES (8701,100,1,1,2,'2024-01-01'),(8702,100,2,1,2,'2025-01-01')
                """);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM primercontactopsicologia WHERE idatencion IN "
                + "(SELECT id FROM atencionalmaaph WHERE codigo='ALMA-2026-987651')");
        jdbc.update("DELETE FROM casopsicologia WHERE codigo='CAS-2026-987651'");
        jdbc.update("DELETE FROM atencionalmaaph WHERE codigo='ALMA-2026-987651'");
        jdbc.update("UPDATE persona SET primernombre='Luis', segundonombre=NULL, "
                + "primerapellido='Pérez', segundoapellido=NULL WHERE id=101");
        jdbc.update("DELETE FROM limiteconsultapreviaaph WHERE idusuario IN (1,2,3,4,5,6,8701)");
        jdbc.update("DELETE FROM auditoriarecursos WHERE idusuario IN (1,2,3,4,5,6,8701)");
        jdbc.update("DELETE FROM usuariorol WHERE idusuario=8701");
        jdbc.update("DELETE FROM usuario WHERE id=8701");
        jdbc.update("DELETE FROM seguimientoatencion WHERE id IN (8701,8702,8703)");
        jdbc.update("DELETE FROM asignacion WHERE id IN (8701,8702)");
        jdbc.update("DELETE FROM atencion WHERE id=8701");
        jdbc.update("DELETE FROM caso WHERE id IN (8701,8702,8703)");
        jdbc.update("DELETE FROM cita WHERE id=8701");
        jdbc.update("DELETE FROM actividad WHERE id=8701");
        jdbc.update("DELETE FROM accion WHERE id=8701");
        jdbc.update("DELETE FROM tiposeguimiento WHERE id=8701");
        jdbc.update("DELETE FROM estadoseguimiento WHERE id IN (8701,8702,8703)");
        jdbc.update("DELETE FROM motivoestadoseguimiento WHERE id=8701");
        jdbc.update("DELETE FROM estadoatencion WHERE id=8701");
        jdbc.update("DELETE FROM estadocaso WHERE id IN (8701,8702)");
        jdbc.update("DELETE FROM estadocita WHERE id=8701");
        jdbc.update("DELETE FROM regimen WHERE id=8701");
        jdbc.update("DELETE FROM eps WHERE id=8701");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder consulta(String documento, String auth) {
        return get("/alma/personas/consulta").param("tipoIdentificacion", "CC")
                .param("numeroIdentificacion", documento).header("Authorization", auth);
    }

    @Test
    void consultaCasoAlmaSinPersonaCentralYNoExponeInformacionClinica() throws Exception {
        Long atencion = jdbc.queryForObject("""
                INSERT INTO atencionalmaaph (codigo,version,estado,datos,fecha_creacion,fecha_actualizacion)
                VALUES ('ALMA-2026-987651',1,'ABIERTO_ACTIVO',
                        '{"persona":{"primerNombre":"María del Carmen","primerApellido":"De la Cruz"}}'::jsonb,
                        now(),now()) RETURNING id
                """, Long.class);
        Long caso = jdbc.queryForObject("""
                INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,
                    numerodocumento,idprofesional,fechaapertura,fechaactualizacion,proximocontrol)
                SELECT 'CAS-2026-987651',1,'ABIERTO','ALTA',id,'1000000999',1,now(),now(),now() + interval '2 days'
                FROM tipoidentificacion WHERE codigo='CC' RETURNING id
                """, Long.class);
        jdbc.update("""
                INSERT INTO primercontactopsicologia (idatencion,idcasopsicologia,idusuario,fecharegistro,datos)
                VALUES (?,?,1,now(),'{"valoracionRiesgo":{"descripcion":"información clínica"}}'::jsonb)
                """, atencion, caso);
        String auth = bearer("consulta.aph@example.edu.co");
        mockMvc.perform(consulta("1000000999", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.persona.idPersona").value(nullValue()))
                .andExpect(jsonPath("$.persona.primerNombre").value("María del Carmen"))
                .andExpect(jsonPath("$.persona.primerApellido").value("De la Cruz"))
                .andExpect(jsonPath("$.seguimientoActivo.activo").value(true))
                .andExpect(jsonPath("$.seguimientoActivo.proximoSeguimiento").isNotEmpty())
                .andExpect(jsonPath("$.seguimientoActivo.profesional").isNotEmpty())
                .andExpect(jsonPath("$.valoracionRiesgo").doesNotExist());
        jdbc.update("UPDATE casopsicologia SET estado='CERRADO' WHERE id=?", caso);
        mockMvc.perform(consulta("1000000999", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.existe").value(false))
                .andExpect(jsonPath("$.seguimientoActivo.activo").value(false))
                .andExpect(jsonPath("$.tieneCasoCerrado").value(false));
    }

    @Test
    void combinaSeguimientosPropiosYExistentesPriorizandoElProximoControl() throws Exception {
        Long atencion = jdbc.queryForObject("""
                INSERT INTO atencionalmaaph (codigo,version,estado,datos,fecha_creacion,fecha_actualizacion)
                VALUES ('ALMA-2026-987651',1,'ABIERTO_ACTIVO','{"persona":{}}'::jsonb,now(),now())
                RETURNING id
                """, Long.class);
        Long caso = jdbc.queryForObject("""
                INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,
                    numerodocumento,idpersona,idprofesional,fechaapertura,fechaactualizacion,proximocontrol)
                SELECT 'CAS-2026-987651',1,'ABIERTO','MEDIA',id,'100000002',101,1,
                       now(),now(),now() + interval '1 hour'
                FROM tipoidentificacion WHERE codigo='CC' RETURNING id
                """, Long.class);
        jdbc.update("""
                INSERT INTO primercontactopsicologia (idatencion,idcasopsicologia,idusuario,fecharegistro,datos)
                VALUES (?,?,1,now(),'{}'::jsonb)
                """, atencion, caso);
        String auth = bearer("consulta.aph@example.edu.co");
        mockMvc.perform(consulta("100000002", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.persona.idPersona").value(101))
                .andExpect(jsonPath("$.seguimientoActivo.profesional").value(
                        jdbc.queryForObject("SELECT nombre FROM usuario WHERE id=1", String.class)));
        jdbc.update("UPDATE casopsicologia SET proximocontrol=now() - interval '1 day' WHERE id=?", caso);
        mockMvc.perform(consulta("100000002", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.seguimientoActivo.profesional").value("Dupla N° 2"));
    }

    @Test
    void entregaSoloDatosMinimosYEligeSeguimientoYFechaConocida() throws Exception {
        jdbc.update("UPDATE persona SET primernombre=?, segundonombre=?, primerapellido=?, segundoapellido=? WHERE id=101",
                "María del Carmen", "Lucía", "De la Cruz", "López");
        String auth = bearer("admin@udea.edu.co");
        String body = mockMvc.perform(consulta("100000002", auth)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.persona.idPersona").value(101))
                .andExpect(jsonPath("$.persona.primerNombre").value("María del Carmen"))
                .andExpect(jsonPath("$.persona.segundoNombre").value("Lucía"))
                .andExpect(jsonPath("$.persona.primerApellido").value("De la Cruz"))
                .andExpect(jsonPath("$.persona.segundoApellido").value("López"))
                .andExpect(jsonPath("$.seguimientoActivo.activo").value(true))
                .andExpect(jsonPath("$.seguimientoActivo.profesional").value("Dupla N° 2"))
                .andExpect(jsonPath("$.ultimoCasoCerrado.numeroCaso").value("CAS-CON-FECHA"))
                .andExpect(jsonPath("$.ultimoCasoCerrado.fechaCierre").value("2024-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.seguimientoActivo.descripcion").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        var fecha = java.time.OffsetDateTime.parse(objectMapper.readTree(body)
                .path("seguimientoActivo").path("proximoSeguimiento").asText());
        var esperada = jdbc.queryForObject("SELECT fecha FROM seguimientoatencion WHERE id=8702",
                java.time.LocalDateTime.class);
        assertEquals(esperada, fecha.toLocalDateTime());
        assertEquals(5, objectMapper.readTree(body).size());
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM auditoriarecursos
                WHERE idusuario=1 AND accion='CONSULTA_PREVIA' AND recurso='PERSONA'
                  AND idrecurso='101' AND tablafectada='persona' AND idregistro='101'
                """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM auditoriarecursos WHERE usuario LIKE '%100000002%'
                   OR idrecurso LIKE '%100000002%' OR idregistro LIKE '%100000002%'
                """, Integer.class));
        mockMvc.perform(consulta("100000001", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.persona.idPersona").value(100))
                .andExpect(jsonPath("$.seguimientoActivo.activo").value(false))
                .andExpect(jsonPath("$.tieneCasoCerrado").value(false));
        mockMvc.perform(consulta("inexistente", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false))
                .andExpect(jsonPath("$.persona").value(nullValue()));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM auditoriarecursos
                WHERE idusuario=1 AND accion='CONSULTA_PREVIA_SIN_COINCIDENCIA'
                  AND recurso='PERSONA' AND idrecurso IS NULL AND idregistro IS NULL
                """, Integer.class));
    }

    @Test
    void eligeVencidoMasRecienteYCreacionCuandoNoHayFechas() throws Exception {
        jdbc.update("UPDATE caso SET fechacierre=NULL WHERE id=8702");
        jdbc.update("UPDATE seguimientoatencion SET fecha=localtimestamp - interval '2 days' WHERE id=8702");
        jdbc.update("UPDATE seguimientoatencion SET idestadoseguimiento=8703 WHERE id=8703");
        String auth = bearer("revisor@udea.edu.co");
        String body = mockMvc.perform(consulta("100000002", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoCasoCerrado.numeroCaso").value("CAS-SIN-FECHA"))
                .andExpect(jsonPath("$.ultimoCasoCerrado.fechaCierre").value(nullValue()))
                .andExpect(jsonPath("$.seguimientoActivo.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        var fecha = java.time.OffsetDateTime.parse(objectMapper.readTree(body).path("seguimientoActivo")
                .path("proximoSeguimiento").asText());
        org.junit.jupiter.api.Assertions.assertTrue(fecha.isAfter(
                java.time.OffsetDateTime.now(java.time.ZoneOffset.ofHours(-5)).minusHours(25)));
    }

    @Test
    void validaParametrosYRestringeRoles() throws Exception {
        mockMvc.perform(get("/alma/personas/consulta")).andExpect(status().isForbidden());
        mockMvc.perform(consulta("100000002", bearer("consulta.aph@example.edu.co")))
                .andExpect(status().isOk());
        for (String email : new String[]{"profesional@udea.edu.co", "coordinador@udea.edu.co", "user@udea.edu.co"}) {
            mockMvc.perform(consulta("100000002", bearer(email))).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/alma/personas/consulta").header("Authorization", bearer("admin@udea.edu.co")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.tipoIdentificacion").isNotEmpty())
                .andExpect(jsonPath("$.detalles.numeroIdentificacion").isNotEmpty());
    }

    @Test
    void solicitudesConcurrentesNoSobrepasanElLimite() throws Exception {
        String auth = bearer("admin@udea.edu.co");
        var ejecutor = java.util.concurrent.Executors.newFixedThreadPool(8);
        try {
            var tareas = new java.util.ArrayList<java.util.concurrent.Callable<Integer>>();
            for (int i = 0; i < 40; i++) {
                tareas.add(() -> mockMvc.perform(consulta("inexistente", auth))
                        .andReturn().getResponse().getStatus());
            }
            var resultados = ejecutor.invokeAll(tareas);
            int exitos = 0;
            int rechazados = 0;
            for (var resultado : resultados) {
                int codigo = resultado.get();
                if (codigo == 200) exitos++;
                if (codigo == 429) rechazados++;
            }
            assertEquals(30, exitos);
            assertEquals(10, rechazados);
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test
    void limitaTreintaSolicitudesPorUsuarioYReiniciaLaVentana() throws Exception {
        String auth = bearer("admin@udea.edu.co");
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(consulta("inexistente", auth)).andExpect(status().isOk());
        }
        mockMvc.perform(consulta("inexistente", auth)).andExpect(status().isTooManyRequests());
        assertEquals(30, jdbc.queryForObject("SELECT cantidad FROM limiteconsultapreviaaph WHERE idusuario=1", Integer.class));
        mockMvc.perform(consulta("inexistente", bearer("revisor@udea.edu.co"))).andExpect(status().isOk());
        jdbc.update("UPDATE limiteconsultapreviaaph SET inicio=clock_timestamp()-interval '11 minutes' WHERE idusuario=1");
        mockMvc.perform(consulta("inexistente", auth)).andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT cantidad FROM limiteconsultapreviaaph WHERE idusuario=1", Integer.class));
    }
}
