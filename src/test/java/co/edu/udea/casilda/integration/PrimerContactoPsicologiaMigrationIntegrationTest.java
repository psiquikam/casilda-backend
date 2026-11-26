package co.edu.udea.casilda.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Savepoint;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrimerContactoPsicologiaMigrationIntegrationTest extends IntegrationTestBase {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    @Transactional
    void creaEsquemaIdempotenteYAdmiteIdentidadSinPersonaCentral() {
        ResourceDatabasePopulator migration = new ResourceDatabasePopulator(
                new FileSystemResource("database/migration_primer_contacto_psicologia.sql"));
        migration.execute(dataSource);
        migration.execute(dataSource);

        Long atencion = jdbc.queryForObject("""
                INSERT INTO atencionalmaaph (codigo, version, estado, datos, fecha_creacion, fecha_actualizacion)
                VALUES ('ALMA-2026-987654', 1, 'PENDIENTE_PSICOLOGIA', '{}'::jsonb, now(), now())
                RETURNING id
                """, Long.class);
        Long caso = jdbc.queryForObject("""
                INSERT INTO casopsicologia (codigo, version, estado, prioridad, idtipoidentificacion,
                    numerodocumento, idprofesional, fechaapertura, fechaactualizacion)
                VALUES ('CAS-2026-987654', 1, 'ABIERTO', 'ALTA', 1, '1234567890', 1, now(), now())
                RETURNING id
                """, Long.class);
        Long otraAtencion = jdbc.queryForObject("""
                INSERT INTO atencionalmaaph (codigo, version, estado, datos, fecha_creacion, fecha_actualizacion)
                VALUES ('ALMA-2026-987655', 1, 'PENDIENTE_PSICOLOGIA', '{}'::jsonb, now(), now())
                RETURNING id
                """, Long.class);
        jdbc.update("INSERT INTO estadocita (id, nombre) VALUES (9801, 'Prueba')");
        jdbc.update("INSERT INTO regimen (id, nombre) VALUES (9801, 'Prueba')");
        jdbc.update("INSERT INTO eps (id, nombre) VALUES (9801, 'Prueba')");
        jdbc.update("""
                INSERT INTO cita (id, idsolicitudatencion, fecha, idestadocita)
                VALUES (9801, 100, CURRENT_TIMESTAMP, 9801)
                """);
        jdbc.update("""
                INSERT INTO caso (id, codigo, idcita, ididentidadgenero, idregimen, ideps)
                VALUES (9801, 'ACO-PRUEBA', 9801, 1, 9801, 9801)
                """);
        jdbc.update("""
                INSERT INTO primercontactopsicologia
                    (idatencion, idcasopsicologia, idusuario, fecharegistro, datos)
                VALUES (?, ?, 1, now(), '{"consentimiento":{"otorgado":true}}'::jsonb)
                """, atencion, caso);
        jdbc.update("""
                INSERT INTO eventocasopsicologia (idcasopsicologia, idusuario, accion, detalle, fecha)
                VALUES (?, 1, 'CREACION', 'Se creó el caso de Psicología.', now())
                """, caso);
        migration.execute(dataSource);

        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM casopsicologia c
                JOIN primercontactopsicologia p ON p.idcasopsicologia=c.id
                JOIN eventocasopsicologia e ON e.idcasopsicologia=c.id
                WHERE c.id=? AND c.idpersona IS NULL AND p.idatencion=?
                """, Integer.class, caso, atencion));

        jdbc.execute((ConnectionCallback<Void>) connection -> {
            Savepoint savepoint = connection.setSavepoint();
            try (var statement = connection.prepareStatement("""
                    INSERT INTO primercontactopsicologia (idatencion, idusuario, fecharegistro, datos)
                    VALUES (?, 1, now(), '{}'::jsonb)
                    """)) {
                statement.setLong(1, otraAtencion);
                assertEquals("23514", assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
            } finally {
                connection.rollback(savepoint);
            }
            savepoint = connection.setSavepoint();
            try (var statement = connection.prepareStatement("""
                    INSERT INTO primercontactopsicologia
                        (idatencion, idcasopsicologia, idusuario, fecharegistro, datos)
                    VALUES (?, ?, 1, now(), '{}'::jsonb)
                    """)) {
                statement.setLong(1, atencion);
                statement.setLong(2, caso);
                assertEquals("23505", assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
            } finally {
                connection.rollback(savepoint);
            }
            savepoint = connection.setSavepoint();
            try (var statement = connection.prepareStatement("""
                    INSERT INTO primercontactopsicologia
                        (idatencion, idcasopsicologia, idcaso, idusuario, fecharegistro, datos)
                    VALUES (?, ?, 9801, 1, now(), '{}'::jsonb)
                    """)) {
                statement.setLong(1, otraAtencion);
                statement.setLong(2, caso);
                assertEquals("23514", assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
            } finally {
                connection.rollback(savepoint);
            }
            return null;
        });
        jdbc.update("""
                INSERT INTO primercontactopsicologia
                    (idatencion, idcaso, idusuario, fecharegistro, datos)
                VALUES (?, 9801, 1, now(), '{}'::jsonb)
                """, otraAtencion);
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM primercontactopsicologia
                WHERE idatencion=? AND idcaso=9801 AND idcasopsicologia IS NULL
                """, Integer.class, otraAtencion));
    }
}
