package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.BandejaPsicologiaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
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
            LEFT JOIN usuario psicologia ON psicologia.id = a.idusuariotomadorpsicologia
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
    private final AtencionAlmaService atencionService;
    private final NovedadPsicologiaService novedadService;

    @Transactional(readOnly = true)
    public BandejaPsicologiaResponse listar(String estado, String triaje, String semaforo,
                                             String documento, int page, int size, Authentication auth) {
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
                       concat_ws(' ', a.datos->'persona'->>'primerNombre',
                           a.datos->'persona'->>'segundoNombre', a.datos->'persona'->>'primerApellido',
                           a.datos->'persona'->>'segundoApellido') AS nombre,
                       psicologia.nombre AS tomada_por,
                       psicologia.email AS tomada_por_email,
                       (SELECT count(*) FROM intentocontactopsicologia i
                        WHERE i.idatencionalmaaph = a.id) AS intentos_realizados,
                       (a.estado = 'PENDIENTE_PSICOLOGIA' AND
                """ + CasosActivosPsicologiaService.EXISTE_PARA_ATENCION + """
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
                            fecha, rs.getString("tomada_por"),
                            auth != null && auth.getName().equalsIgnoreCase(rs.getString("tomada_por_email")),
                            rs.getBoolean("recontacto"), rs.getInt("intentos_realizados"),
                            SlaPsicologia.calcular(nivel, fecha, ahora));
        });
        return new BandejaPsicologiaResponse(items, page, size, Objects.requireNonNull(total));
    }

    @Transactional
    public JsonNode tomar(String idAtencion, JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, Set.of("PROFESIONAL", "COORDINADOR", "ADMIN"));
        ObjectNode cuerpo = atencionService.comprobarObjeto(request, "", Set.of("version"));
        validarVersionFormato(cuerpo.get("version"));
        AtencionAlmaAph atencion = atencionService.buscar(idAtencion, true);
        verificarNovedadPsicologia(atencion);
        if (!"PENDIENTE_PSICOLOGIA".equals(atencion.getEstado())) {
            throw novedadCerrada();
        }

        Usuario actor = atencionService.usuario(auth);
        if (atencion.getTomadaPorPsicologia() != null) {
            if (atencion.getTomadaPorPsicologia().getId().equals(actor.getId())) {
                return respuestaPsicologia(atencion, auth);
            }
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "NOVEDAD_YA_TOMADA",
                            "Otra profesional ya tomó esta novedad.",
                            Map.of("tomadaPor", atencion.getTomadaPorPsicologia().getNombre(),
                                    "tomadaEn", atencion.getFechaTomaPsicologia() == null
                                            ? "" : atencion.getFechaTomaPsicologia().toString()));
        }
        validarVersion(atencion, cuerpo.get("version"));

        Instant ahora = Instant.now();
        atencion.setTomadaPorPsicologia(actor);
        atencion.setFechaTomaPsicologia(ahora);
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        atencionService.registrarEvento(atencion, actor, "TOMA_PSICOLOGIA", ahora);
        return respuestaPsicologia(atencion, auth);
    }

    @Transactional
    public JsonNode liberar(String idAtencion, JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, Set.of("PROFESIONAL", "COORDINADOR", "ADMIN"));
        ObjectNode cuerpo = atencionService.comprobarObjeto(request, "", Set.of("version"));
        AtencionAlmaAph atencion = atencionService.buscar(idAtencion, true);
        verificarNovedadPsicologia(atencion);
        if (!"PENDIENTE_PSICOLOGIA".equals(atencion.getEstado())) {
            throw novedadCerrada();
        }
        Usuario actor = atencionService.usuario(auth);
        if (atencion.getTomadaPorPsicologia() == null
                || !atencion.getTomadaPorPsicologia().getId().equals(actor.getId())) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "NOVEDAD_NO_TOMADA",
                            "Solo quien tomó la novedad puede liberarla.",
                            Map.of("tomadaPor", atencion.getTomadaPorPsicologia() == null
                                    ? "" : atencion.getTomadaPorPsicologia().getNombre()));
        }
        validarVersion(atencion, cuerpo.get("version"));

        Instant ahora = Instant.now();
        atencion.setTomadaPorPsicologia(null);
        atencion.setFechaTomaPsicologia(null);
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        atencionService.registrarEvento(atencion, actor, "LIBERACION_PSICOLOGIA", ahora);
        return respuestaPsicologia(atencion, auth);
    }

    private JsonNode respuestaPsicologia(AtencionAlmaAph atencion, Authentication auth) {
        return novedadService.respuestaPsicologia(atencion, auth);
    }

    private void verificarNovedadPsicologia(AtencionAlmaAph atencion) {
        if (atencion.getFechaDerivacion() == null) {
            throw new AtencionAlmaException(HttpStatus.NOT_FOUND, "NOVEDAD_NO_ENCONTRADA",
                            "No se encontró una novedad derivada a Psicología.", Map.of());
        }
    }

    private void validarVersion(AtencionAlmaAph atencion, JsonNode version) {
        validarVersionFormato(version);
        if (version.intValue() != atencion.getVersion()) {
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "CONFLICTO_VERSION",
                            "La novedad cambió. Recargue antes de continuar.",
                            Map.of("versionActual", atencion.getVersion()));
        }
    }

    private void validarVersionFormato(JsonNode version) {
        if (version == null || !version.isIntegralNumber() || !version.canConvertToInt()) {
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "CONFLICTO_VERSION",
                            "Indique la versión actual de la novedad.", Map.of());
        }
    }

    private AtencionAlmaException novedadCerrada() {
        return new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "NOVEDAD_CERRADA",
                "La novedad ya no está pendiente.", Map.of());
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
