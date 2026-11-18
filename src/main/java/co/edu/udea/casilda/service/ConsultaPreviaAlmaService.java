package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.ConsultaPreviaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.repository.PersonaRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConsultaPreviaAlmaService {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private final PersonaRepository personas;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public ConsultaPreviaAlmaResponse consultar(String tipo, String numero) {
        Map<String, String> errores = new java.util.LinkedHashMap<>();
        if (!StringUtils.hasText(tipo)) errores.put("tipoIdentificacion", "Seleccione el tipo de documento.");
        if (!StringUtils.hasText(numero)) errores.put("numeroIdentificacion", "Escriba el número de documento.");
        if (!errores.isEmpty()) {
            throw new AtencionAlmaException(HttpStatus.BAD_REQUEST, "VALIDACION",
                    "Revise los datos marcados e intente de nuevo.", errores);
        }

        Persona persona = personas.findByNumeroDocumentoAndTipoIdentificacion_Codigo(numero.trim(), tipo.trim())
                .orElse(null);
        var sinSeguimiento = new ConsultaPreviaAlmaResponse.SeguimientoActivo(false, null, null);
        if (persona == null) return new ConsultaPreviaAlmaResponse(false, null, sinSeguimiento, false, null);

        var datos = new ConsultaPreviaAlmaResponse.Persona(persona.getId(),
                nombre(persona.getPrimerNombre(), persona.getSegundoNombre()),
                nombre(persona.getPrimerApellido(), persona.getSegundoApellido()));
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        List<ConsultaPreviaAlmaResponse.SeguimientoActivo> seguimientos = jdbc.query("""
                SELECT seg.fecha, gp.nombre AS profesional
                FROM seguimientoatencion seg
                JOIN accion a ON a.id = seg.idaccion
                JOIN estadoseguimiento es ON es.id = seg.idestadoseguimiento
                JOIN atencion at ON at.id = seg.idatencion
                JOIN caso c ON c.id = at.idcaso
                JOIN estadocaso ec ON ec.id = c.idestadocaso
                JOIN cita ci ON ci.id = c.idcita
                JOIN solicitudatencion s ON s.id = ci.idsolicitudatencion
                LEFT JOIN remision rem ON rem.id = s.idremision
                LEFT JOIN LATERAL (
                    SELECT asign.idgrupoprofesional FROM asignacion asign
                    WHERE asign.idsolicitudatencion = s.id
                    ORDER BY asign.fechacreacion DESC NULLS LAST, asign.id DESC LIMIT 1
                ) vigente ON true
                LEFT JOIN grupoprofesional gp ON gp.id = vigente.idgrupoprofesional
                WHERE COALESCE(rem.idremitente, s.idsolicitante) = ?
                  AND ec.nombre IN ('Abierto activo', 'Abierto en transición')
                  AND es.nombre IN ('En Proceso', 'Pendiente')
                  AND a.nombre = 'Seguimiento por psicología'
                ORDER BY CASE WHEN seg.fecha >= ? THEN 0 ELSE 1 END,
                         CASE WHEN seg.fecha >= ? THEN seg.fecha END ASC,
                         CASE WHEN seg.fecha < ? THEN seg.fecha END DESC, seg.id DESC
                LIMIT 1
                """, (rs, row) -> new ConsultaPreviaAlmaResponse.SeguimientoActivo(true,
                        rs.getObject("fecha", LocalDateTime.class).atZone(ZONA).toOffsetDateTime(),
                        rs.getString("profesional")), persona.getId(), ahora, ahora, ahora);
        List<ConsultaPreviaAlmaResponse.CasoCerrado> casos = jdbc.query("""
                SELECT c.codigo, c.fechacierre
                FROM caso c
                JOIN estadocaso ec ON ec.id = c.idestadocaso
                JOIN cita ci ON ci.id = c.idcita
                JOIN solicitudatencion s ON s.id = ci.idsolicitudatencion
                LEFT JOIN remision rem ON rem.id = s.idremision
                WHERE COALESCE(rem.idremitente, s.idsolicitante) = ? AND ec.nombre = 'Cerrado'
                ORDER BY c.fechacierre DESC NULLS LAST, c.fechacreacion DESC NULLS LAST, c.id DESC
                LIMIT 1
                """, (rs, row) -> new ConsultaPreviaAlmaResponse.CasoCerrado(rs.getString("codigo"),
                        rs.getTimestamp("fechacierre") == null ? null : rs.getTimestamp("fechacierre").toInstant()),
                persona.getId());
        return new ConsultaPreviaAlmaResponse(true, datos,
                seguimientos.isEmpty() ? sinSeguimiento : seguimientos.getFirst(),
                !casos.isEmpty(), casos.isEmpty() ? null : casos.getFirst());
    }

    private String nombre(String primero, String segundo) {
        return StringUtils.hasText(segundo) ? primero + " " + segundo : primero;
    }
}
