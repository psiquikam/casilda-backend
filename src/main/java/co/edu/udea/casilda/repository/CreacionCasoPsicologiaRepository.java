package co.edu.udea.casilda.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordina la creación de casos de Psicología dentro de la transacción del primer contacto.
 */
@Repository
@RequiredArgsConstructor
public class CreacionCasoPsicologiaRepository {
    private final JdbcTemplate jdbc;

    /**
     * Serializa los primeros contactos de una misma identidad hasta finalizar la transacción.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void bloquearIdentidad(String tipoIdentificacion, String numeroIdentificacion) {
        jdbc.query("""
                SELECT pg_advisory_xact_lock(hashtextextended(
                    'primer-contacto-psicologia:' || length(?) || ':' || ? || ':' || upper(?), 0))
                """, rs -> null, tipoIdentificacion, tipoIdentificacion, numeroIdentificacion);
    }

    /**
     * Reserva un código anual y evita códigos ya usados en cualquiera de los dos modelos.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String reservarCodigo(Instant fecha) {
        int anio = fecha.atZone(ZoneOffset.UTC).getYear();
        while (true) {
            List<Integer> numeros = jdbc.queryForList("""
                    INSERT INTO consecutivocasopsicologia (anio, ultimo) VALUES (?, 1)
                    ON CONFLICT (anio) DO UPDATE SET ultimo = consecutivocasopsicologia.ultimo + 1
                    WHERE consecutivocasopsicologia.ultimo < 999999
                    RETURNING ultimo
                    """, Integer.class, anio);
            if (numeros.isEmpty()) {
                throw new IllegalStateException("Se agotó el consecutivo anual de casos de Psicología");
            }
            String codigo = "CAS-%04d-%06d".formatted(anio, numeros.getFirst());
            Boolean existe = jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM casopsicologia WHERE codigo = ?)
                        OR EXISTS(SELECT 1 FROM caso WHERE codigo = ?)
                    """, Boolean.class, codigo, codigo);
            if (Boolean.FALSE.equals(existe)) {
                return codigo;
            }
            if (existe == null) {
                throw new IllegalStateException("No se pudo verificar el código de caso");
            }
        }
    }
}
