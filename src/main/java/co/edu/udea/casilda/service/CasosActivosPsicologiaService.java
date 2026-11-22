package co.edu.udea.casilda.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta casos activos propios de Línea Alma y casos existentes de la misma persona.
 */
@Service
@RequiredArgsConstructor
public class CasosActivosPsicologiaService {
    static final String EXISTE_PARA_ATENCION = """
            (EXISTS (
                SELECT 1 FROM casopsicologia cp
                JOIN tipoidentificacion ti ON ti.id=cp.idtipoidentificacion
                WHERE cp.estado IN ('ABIERTO', 'ACTIVO', 'EN_CIERRE')
                  AND ti.codigo=a.datos->'persona'->>'tipoIdentificacion'
                  AND cp.numerodocumento=a.datos->'persona'->>'numeroIdentificacion'
            ) OR EXISTS (
                SELECT 1 FROM caso c JOIN estadocaso ec ON ec.id=c.idestadocaso
                JOIN cita ci ON ci.id=c.idcita
                JOIN solicitudatencion s ON s.id=ci.idsolicitudatencion
                LEFT JOIN remision rem ON rem.id=s.idremision
                JOIN persona p ON p.id=COALESCE(rem.idremitente, s.idsolicitante)
                JOIN tipoidentificacion ti ON ti.id=p.idtipoidentificacion
                WHERE ec.nombre IN ('Abierto activo', 'Abierto en transición')
                  AND ti.codigo=a.datos->'persona'->>'tipoIdentificacion'
                  AND p.numerodocumento=a.datos->'persona'->>'numeroIdentificacion'
            ))
            """;
    private final JdbcTemplate jdbc;

    public record CasoActivo(String numeroCaso, String estado, Instant fechaApertura,
                             String profesional, long numeroSeguimientos, Instant proximoControl) {}

    @Transactional(readOnly = true)
    public List<CasoActivo> buscar(String tipoIdentificacion, String numeroIdentificacion) {
        return jdbc.query("""
                SELECT resultado.codigo, resultado.estado, resultado.apertura,
                       resultado.profesional, resultado.seguimientos, resultado.proximocontrol
                FROM (
                    SELECT c.codigo, c.estado, c.fechaapertura AS apertura, u.nombre AS profesional,
                           0::bigint AS seguimientos, c.proximocontrol
                    FROM casopsicologia c
                    JOIN tipoidentificacion ti ON ti.id = c.idtipoidentificacion
                    JOIN usuario u ON u.id = c.idprofesional
                    WHERE ti.codigo = ? AND c.numerodocumento = ?
                      AND c.estado IN ('ABIERTO', 'ACTIVO', 'EN_CIERRE')
                    UNION ALL
                    SELECT c.codigo, 'ACTIVO' AS estado,
                           c.fechacreacion AT TIME ZONE 'America/Bogota' AS apertura,
                           gp.nombre AS profesional,
                           (SELECT count(*) FROM seguimientoatencion seg
                            JOIN atencion at ON at.id = seg.idatencion
                            JOIN accion ac ON ac.id = seg.idaccion
                            WHERE at.idcaso = c.id AND ac.nombre = 'Seguimiento por psicología') AS seguimientos,
                           (SELECT min(seg.fecha) AT TIME ZONE 'America/Bogota'
                            FROM seguimientoatencion seg
                            JOIN atencion at ON at.id = seg.idatencion
                            JOIN accion ac ON ac.id = seg.idaccion
                            JOIN estadoseguimiento es ON es.id = seg.idestadoseguimiento
                            WHERE at.idcaso = c.id AND ac.nombre = 'Seguimiento por psicología'
                              AND es.nombre IN ('En Proceso', 'Pendiente')
                              AND seg.fecha >= (current_timestamp AT TIME ZONE 'America/Bogota'))
                               AS proximocontrol
                    FROM caso c
                    JOIN estadocaso ec ON ec.id = c.idestadocaso
                    JOIN cita ci ON ci.id = c.idcita
                    JOIN solicitudatencion s ON s.id = ci.idsolicitudatencion
                    LEFT JOIN remision rem ON rem.id = s.idremision
                    JOIN persona p ON p.id = COALESCE(rem.idremitente, s.idsolicitante)
                    JOIN tipoidentificacion ti ON ti.id = p.idtipoidentificacion
                    LEFT JOIN LATERAL (
                        SELECT asign.idgrupoprofesional FROM asignacion asign
                        WHERE asign.idsolicitudatencion = s.id
                        ORDER BY asign.fechacreacion DESC NULLS LAST, asign.id DESC LIMIT 1
                    ) vigente ON true
                    LEFT JOIN grupoprofesional gp ON gp.id = vigente.idgrupoprofesional
                    WHERE ti.codigo = ? AND p.numerodocumento = ?
                      AND ec.nombre IN ('Abierto activo', 'Abierto en transición')
                ) resultado
                ORDER BY resultado.apertura DESC NULLS LAST, resultado.codigo
                """, (rs, row) -> new CasoActivo(
                rs.getString("codigo"), rs.getString("estado"),
                rs.getTimestamp("apertura") == null ? null : rs.getTimestamp("apertura").toInstant(),
                rs.getString("profesional"), rs.getLong("seguimientos"),
                rs.getTimestamp("proximocontrol") == null ? null : rs.getTimestamp("proximocontrol").toInstant()),
                tipoIdentificacion, numeroIdentificacion, tipoIdentificacion, numeroIdentificacion);
    }
}
