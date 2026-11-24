package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.ConsultaPreviaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.repository.PersonaRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

        Persona persona = personas.findByNumeroDocumentoIgnoreCaseAndTipoIdentificacion_Codigo(numero.trim(), tipo.trim())
                .orElse(null);
        var sinSeguimiento = new ConsultaPreviaAlmaResponse.SeguimientoActivo(false, null, null);
        var casosAlma = jdbc.query("""
                SELECT c.idpersona, u.nombre AS profesional, c.proximocontrol,
                       a.datos->'persona'->>'primerNombre' AS primernombre,
                       a.datos->'persona'->>'segundoNombre' AS segundonombre,
                       a.datos->'persona'->>'primerApellido' AS primerapellido,
                       a.datos->'persona'->>'segundoApellido' AS segundoapellido
                FROM casopsicologia c
                JOIN tipoidentificacion ti ON ti.id = c.idtipoidentificacion
                JOIN usuario u ON u.id = c.idprofesional
                JOIN LATERAL (
                    SELECT pc.idatencion FROM primercontactopsicologia pc
                    WHERE pc.idcasopsicologia = c.id ORDER BY pc.fecharegistro DESC, pc.id DESC LIMIT 1
                ) contacto ON true
                JOIN atencionalmaaph a ON a.id = contacto.idatencion
                WHERE ti.codigo = ? AND upper(c.numerodocumento) = upper(?)
                  AND c.estado IN ('ABIERTO', 'ACTIVO', 'EN_CIERRE')
                ORDER BY CASE WHEN c.proximocontrol >= current_timestamp THEN 0
                              WHEN c.proximocontrol IS NOT NULL THEN 1 ELSE 2 END,
                         CASE WHEN c.proximocontrol >= current_timestamp THEN c.proximocontrol END,
                         CASE WHEN c.proximocontrol < current_timestamp THEN c.proximocontrol END DESC,
                         c.fechaapertura DESC, c.id DESC
                LIMIT 1
                """, (rs, row) -> new DatosCasoAlma(
                new ConsultaPreviaAlmaResponse.Persona(rs.getObject("idpersona", Long.class),
                        rs.getString("primernombre"), rs.getString("segundonombre"),
                        rs.getString("primerapellido"), rs.getString("segundoapellido")),
                new ConsultaPreviaAlmaResponse.SeguimientoActivo(true,
                        rs.getTimestamp("proximocontrol") == null ? null
                                : rs.getTimestamp("proximocontrol").toInstant().atZone(ZONA).toOffsetDateTime(),
                        rs.getString("profesional"))), tipo.trim(), numero.trim());
        if (persona == null) {
            var datosAlma = casosAlma.isEmpty() ? null : casosAlma.getFirst().persona();
            return new ConsultaPreviaAlmaResponse(datosAlma != null, datosAlma,
                    casosAlma.isEmpty() ? sinSeguimiento : casosAlma.getFirst().seguimiento(),
                    false, null);
        }

        var datos = new ConsultaPreviaAlmaResponse.Persona(persona.getId(),
                persona.getPrimerNombre(), persona.getSegundoNombre(),
                persona.getPrimerApellido(), persona.getSegundoApellido());
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
        var seguimiento = seguimientos.isEmpty() ? sinSeguimiento : seguimientos.getFirst();
        if (!casosAlma.isEmpty() && preferir(casosAlma.getFirst().seguimiento(), seguimiento)) {
            seguimiento = casosAlma.getFirst().seguimiento();
        }
        List<ConsultaPreviaAlmaResponse.SeguimientoActivo> contactosExistentes = jdbc.query("""
                SELECT DISTINCT ON (c.id) pc.datos->>'proximoControl' AS control, u.nombre AS profesional
                FROM primercontactopsicologia pc
                JOIN caso c ON c.id=pc.idcaso
                JOIN estadocaso ec ON ec.id=c.idestadocaso
                JOIN cita ci ON ci.id=c.idcita
                JOIN solicitudatencion s ON s.id=ci.idsolicitudatencion
                LEFT JOIN remision rem ON rem.id=s.idremision
                JOIN usuario u ON u.id=pc.idusuario
                WHERE COALESCE(rem.idremitente,s.idsolicitante)=?
                  AND ec.nombre IN ('Abierto activo','Abierto en transición')
                  AND pc.datos->>'proximoControl' IS NOT NULL
                ORDER BY c.id, pc.fecharegistro DESC, pc.id DESC
                """, (rs, row) -> new ConsultaPreviaAlmaResponse.SeguimientoActivo(true,
                OffsetDateTime.parse(rs.getString("control")).atZoneSameInstant(ZONA).toOffsetDateTime(),
                rs.getString("profesional")), persona.getId());
        for (var contacto : contactosExistentes) {
            if (preferir(contacto, seguimiento)) seguimiento = contacto;
        }
        var cerrado = casos.isEmpty() ? null : casos.getFirst();
        return new ConsultaPreviaAlmaResponse(true, datos, seguimiento, cerrado != null, cerrado);
    }

    private boolean preferir(ConsultaPreviaAlmaResponse.SeguimientoActivo candidato,
                            ConsultaPreviaAlmaResponse.SeguimientoActivo actual) {
        if (!actual.activo()) return true;
        OffsetDateTime primero = candidato.proximoSeguimiento();
        OffsetDateTime segundo = actual.proximoSeguimiento();
        if (primero == null) return false;
        if (segundo == null) return true;
        OffsetDateTime ahora = OffsetDateTime.now(ZONA);
        boolean primeroFuturo = !primero.isBefore(ahora);
        boolean segundoFuturo = !segundo.isBefore(ahora);
        if (primeroFuturo != segundoFuturo) return primeroFuturo;
        return primeroFuturo ? primero.isBefore(segundo) : primero.isAfter(segundo);
    }

    private record DatosCasoAlma(ConsultaPreviaAlmaResponse.Persona persona,
                                 ConsultaPreviaAlmaResponse.SeguimientoActivo seguimiento) {}
}
