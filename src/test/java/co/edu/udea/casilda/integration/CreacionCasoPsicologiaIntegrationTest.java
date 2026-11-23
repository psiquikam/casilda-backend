package co.edu.udea.casilda.integration;

import co.edu.udea.casilda.repository.CreacionCasoPsicologiaRepository;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreacionCasoPsicologiaIntegrationTest extends IntegrationTestBase {
    @Autowired private CreacionCasoPsicologiaRepository creacion;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void preparar() {
        new ResourceDatabasePopulator(new FileSystemResource(
                "database/migration_primer_contacto_psicologia.sql")).execute(dataSource);
    }

    @Test
    @Transactional
    void reservaCodigosAnualesSinColisionConCasosDeAmbosModelos() {
        jdbc.update("INSERT INTO estadocita (id,nombre) VALUES (9861,'Prueba')");
        jdbc.update("INSERT INTO regimen (id,nombre) VALUES (9861,'Prueba')");
        jdbc.update("INSERT INTO eps (id,nombre) VALUES (9861,'Prueba')");
        jdbc.update("INSERT INTO cita (id,idsolicitudatencion,fecha,idestadocita) VALUES (9861,100,now(),9861)");
        jdbc.update("""
                INSERT INTO caso (id,codigo,idcita,ididentidadgenero,idregimen,ideps)
                VALUES (9861,'CAS-2097-000001',9861,1,9861,9861)
                """);
        jdbc.update("""
                INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,
                    numerodocumento,idprofesional,fechaapertura,fechaactualizacion)
                SELECT 'CAS-2097-000002',1,'ABIERTO','ALTA',id,'1234567890',1,now(),now()
                FROM tipoidentificacion WHERE codigo='CC'
                """);
        Instant fecha = Instant.parse("2097-01-01T00:00:00Z");
        assertEquals("CAS-2097-000003", creacion.reservarCodigo(fecha));
        assertEquals("CAS-2097-000004", creacion.reservarCodigo(fecha));
        assertEquals("CAS-2098-000001", creacion.reservarCodigo(Instant.parse("2098-01-01T00:00:00Z")));
    }

    @Test
    @Transactional
    void agotamientoNoGeneraUnCodigoInvalido() {
        jdbc.update("INSERT INTO consecutivocasopsicologia (anio,ultimo) VALUES (2099,999999)");
        assertThrows(InvalidDataAccessApiUsageException.class,
                () -> creacion.reservarCodigo(Instant.parse("2099-01-01T00:00:00Z")));
    }

    @Test
    void exigeTransaccionParaReservarYBloquear() {
        assertThrows(IllegalTransactionStateException.class, () -> creacion.bloquearIdentidad("CC", "1234567890"));
        assertThrows(IllegalTransactionStateException.class,
                () -> creacion.reservarCodigo(Instant.parse("2097-01-01T00:00:00Z")));
    }

    @Test
    void revierteLaReservaSiFallaLaTransaccion() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Instant fecha = Instant.parse("2095-01-01T00:00:00Z");
        try {
            assertEquals("CAS-2095-000001", tx.execute(status -> {
                String codigo = creacion.reservarCodigo(fecha);
                status.setRollbackOnly();
                return codigo;
            }));
            assertEquals("CAS-2095-000001", tx.execute(status -> creacion.reservarCodigo(fecha)));
        } finally {
            jdbc.update("DELETE FROM consecutivocasopsicologia WHERE anio=2095");
        }
    }

    @Test
    void reservaCodigosDistintosEnTransaccionesConcurrentes() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Instant fecha = Instant.parse("2094-01-01T00:00:00Z");
        try {
            var tareas = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                if (!inicio.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se pudo iniciar la prueba");
                }
                return tx.execute(status -> creacion.reservarCodigo(fecha));
            })).toList();
            inicio.countDown();
            assertEquals(List.of("CAS-2094-000001", "CAS-2094-000002"),
                    List.of(tareas.get(0).get(10, TimeUnit.SECONDS), tareas.get(1).get(10, TimeUnit.SECONDS))
                            .stream().sorted().toList());
        } finally {
            inicio.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
            jdbc.update("DELETE FROM consecutivocasopsicologia WHERE anio=2094");
        }
    }

    @Test
    void serializaPrimerosContactosDeLaMismaPersona() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch primeroBloqueo = new CountDownLatch(1);
        CountDownLatch permitirCommit = new CountDownLatch(1);
        CountDownLatch segundoInicio = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try {
            var primero = executor.submit(() -> tx.execute(status -> {
                creacion.bloquearIdentidad("CC", "1234567890");
                jdbc.update("""
                        INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,
                            numerodocumento,idprofesional,fechaapertura,fechaactualizacion)
                        SELECT 'CAS-2096-999999',1,'ABIERTO','ALTA',id,'1234567890',1,now(),now()
                        FROM tipoidentificacion WHERE codigo='CC'
                        """);
                primeroBloqueo.countDown();
                try {
                    if (!permitirCommit.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se liberó la primera transacción");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Prueba interrumpida", ex);
                }
                return true;
            }));
            assertTrue(primeroBloqueo.await(10, TimeUnit.SECONDS));
            var segundo = executor.submit(() -> tx.execute(status -> {
                segundoInicio.countDown();
                creacion.bloquearIdentidad("CC", "1234567890");
                return jdbc.queryForObject("SELECT count(*) FROM casopsicologia WHERE codigo='CAS-2096-999999'",
                        Integer.class);
            }));
            assertTrue(segundoInicio.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> segundo.get(1, TimeUnit.SECONDS));
            permitirCommit.countDown();
            assertTrue(primero.get(10, TimeUnit.SECONDS));
            assertEquals(1, segundo.get(10, TimeUnit.SECONDS));
        } finally {
            permitirCommit.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
            jdbc.update("DELETE FROM casopsicologia WHERE codigo='CAS-2096-999999'");
        }
    }

    @Test
    void noBloqueaIdentidadesDiferentes() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch primeroBloqueo = new CountDownLatch(1);
        CountDownLatch permitirCommit = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try {
            var primero = executor.submit(() -> tx.execute(status -> {
                creacion.bloquearIdentidad("CC", "1234567890");
                primeroBloqueo.countDown();
                try {
                    if (!permitirCommit.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se liberó la primera transacción");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Prueba interrumpida", ex);
                }
                return true;
            }));
            assertTrue(primeroBloqueo.await(10, TimeUnit.SECONDS));
            var segundo = executor.submit(() -> tx.execute(status -> {
                creacion.bloquearIdentidad("CC", "1234567891");
                return true;
            }));
            assertTrue(segundo.get(5, TimeUnit.SECONDS));
            permitirCommit.countDown();
            assertTrue(primero.get(10, TimeUnit.SECONDS));
        } finally {
            permitirCommit.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }
}
