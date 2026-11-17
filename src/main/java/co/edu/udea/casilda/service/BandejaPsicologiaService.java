package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.BandejaPsicologiaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BandejaPsicologiaService {
    private static final String FILTROS = """
            FROM atencionalmaaph a
            WHERE a.estado = :estado AND a.fechaderivacion IS NOT NULL
              AND (:estado <> 'CERRADO' OR a.datos->'cierre'->>'motivo' = 'NO_CONTACTO')
              AND (:triaje = '' OR a.triaje = :triaje)
              AND (:documento = '' OR strpos(a.datos->'persona'->>'numeroIdentificacion', :documento) > 0)
              AND (:semaforo = '' OR
                   CASE
                     WHEN extract(epoch FROM (a.fechaderivacion +
                       (CASE a.triaje WHEN 'ALTO' THEN 12 WHEN 'MEDIO' THEN 24 ELSE 48 END) * interval '1 hour'
                       - CAST(:ahora AS timestamptz))) < (CASE a.triaje WHEN 'ALTO' THEN 12 WHEN 'MEDIO' THEN 24 ELSE 48 END) * 720
                       THEN 'ROJO'
                     WHEN extract(epoch FROM (a.fechaderivacion +
                       (CASE a.triaje WHEN 'ALTO' THEN 12 WHEN 'MEDIO' THEN 24 ELSE 48 END) * interval '1 hour'
                       - CAST(:ahora AS timestamptz))) <= (CASE a.triaje WHEN 'ALTO' THEN 12 WHEN 'MEDIO' THEN 24 ELSE 48 END) * 1800
                       THEN 'AMARILLO'
                     ELSE 'VERDE'
                   END = :semaforo)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public BandejaPsicologiaResponse listar(String estado, String triaje, String semaforo,
                                             String documento, int page, int size) {
        validar("estado", estado, Set.of("PENDIENTE_PSICOLOGIA", "CERRADO"));
        validar("triaje", triaje, Set.of("ALTO", "MEDIO", "BAJO"));
        validar("semaforo", semaforo, Set.of("VERDE", "AMARILLO", "ROJO"));
        if (page < 0 || size < 1 || size > 100) {
            throw error(Map.of("page", "Use una página válida y un tamaño entre 1 y 100."));
        }
        Instant ahora = Instant.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("estado", StringUtils.hasText(estado) ? estado : "PENDIENTE_PSICOLOGIA")
                .addValue("triaje", triaje == null ? "" : triaje)
                .addValue("semaforo", semaforo == null ? "" : semaforo)
                .addValue("documento", documento == null ? "" : documento)
                .addValue("ahora", java.time.OffsetDateTime.ofInstant(ahora, java.time.ZoneOffset.UTC))
                .addValue("limit", size).addValue("offset", (long) page * size);
        Long total = jdbc.queryForObject("SELECT count(*) " + FILTROS, params, Long.class);
        String sql = """
                SELECT a.codigo, a.version, a.estado, a.triaje, a.fechaderivacion,
                       a.datos->>'canalIngreso' AS canal,
                       a.datos->'persona'->>'tipoIdentificacion' AS tipo,
                       a.datos->'persona'->>'numeroIdentificacion' AS documento,
                       concat_ws(' ', a.datos->'persona'->>'nombres', a.datos->'persona'->>'apellidos') AS nombre,
                       EXISTS (
                           SELECT 1 FROM caso c JOIN estadocaso ec ON ec.id=c.idestadocaso
                           JOIN cita ci ON ci.id=c.idcita
                           JOIN solicitudatencion s ON s.id=ci.idsolicitudatencion
                           LEFT JOIN remision rem ON rem.id=s.idremision
                           JOIN persona p ON p.id=COALESCE(rem.idremitente, s.idsolicitante)
                           JOIN tipoidentificacion ti ON ti.id=p.idtipoidentificacion
                           WHERE ec.nombre IN ('Abierto activo', 'Abierto en transición')
                             AND ti.codigo=a.datos->'persona'->>'tipoIdentificacion'
                             AND p.numerodocumento=a.datos->'persona'->>'numeroIdentificacion'
                       ) AS recontacto
                """ + FILTROS + """
                ORDER BY a.fechaderivacion +
                  (CASE a.triaje WHEN 'ALTO' THEN 12 WHEN 'MEDIO' THEN 24 ELSE 48 END) * interval '1 hour',
                  a.codigo
                LIMIT :limit OFFSET :offset
                """;
        List<BandejaPsicologiaResponse.Item> items = jdbc.query(sql, params, (rs, row) -> {
            Instant fecha = rs.getTimestamp("fechaderivacion").toInstant();
            String nivel = rs.getString("triaje");
            return new BandejaPsicologiaResponse.Item(rs.getString("codigo"), rs.getInt("version"),
                    new BandejaPsicologiaResponse.Persona(rs.getString("tipo"), rs.getString("documento"),
                            rs.getString("nombre").trim()), nivel, rs.getString("canal"), rs.getString("estado"),
                    fecha, null, false, rs.getBoolean("recontacto"), 0, SlaPsicologia.calcular(nivel, fecha, ahora));
        });
        return new BandejaPsicologiaResponse(items, page, size, Objects.requireNonNull(total));
    }

    private void validar(String campo, String valor, Set<String> valores) {
        if (valor != null && !valor.isEmpty() && !valores.contains(valor)) {
            throw error(Map.of(campo, "Seleccione un valor válido."));
        }
    }

    private AtencionAlmaException error(Map<String, String> detalles) {
        return new AtencionAlmaException(HttpStatus.BAD_REQUEST, "VALIDACION", "Revise los filtros.", detalles);
    }
}
