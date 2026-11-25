package co.edu.udea.casilda.service;

import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.CreacionCasoPsicologiaRepository;
import co.edu.udea.casilda.repository.EstadoCasoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Gestión de decisiones de Psicología sobre recontactos. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecontactoPsicologiaService {
    private static final Set<String> LECTURA = Set.of("PROFESIONAL", "COORDINADOR", "ADMIN", "REVISOR");
    private static final Set<String> ESCRITURA = Set.of("PROFESIONAL", "COORDINADOR", "ADMIN");
    private final AtencionAlmaService atenciones;
    private final NovedadPsicologiaService novedades;
    private final CasosActivosPsicologiaService candidatos;
    private final CreacionCasoPsicologiaRepository creacion;
    private final CasoRepository casos;
    private final EstadoCasoRepository estados;
    private final AuditoriaCasoService auditoria;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public record Persona(String tipoIdentificacion, String numeroIdentificacion, String nombreCompleto) {}
    public record CasoRecontacto(String numeroCaso, String estado, Instant fechaApertura, String profesional,
                                  long numeroSeguimientos, Instant proximoControl, String ultimaNota) {}
    public record Recontacto(String idAtencion, int version, Instant fechaCreacion, String canalIngreso,
                             String triaje, String motivoConsulta, String notaAph, Persona persona,
                             List<CasoRecontacto> casosDisponibles,
                             boolean tomadaPorMi, String tomadaPor) {}
    public record Resolucion(String idAtencion, int version, String decision, String numeroCaso,
                             String estadoAtencion, boolean reabrioCaso) {}
    public record DetalleResolucion(String idAtencion, String decision, String numeroCaso, String nota,
                                    boolean reabrioCaso, Instant fecha, String autor) {}

    @Transactional(readOnly = true)
    public List<Recontacto> listar(Authentication auth) {
        atenciones.verificarRol(auth, LECTURA);
        Long usuario = atenciones.usuario(auth).getId();
        return jdbc.query("""
                SELECT a.codigo,a.version,a.fecha_creacion,a.datos,a.triaje,
                       a.idusuariotomadorpsicologia,u.nombre AS tomada_por
                FROM atencionalmaaph a
                LEFT JOIN usuario u ON u.id=a.idusuariotomadorpsicologia
                WHERE a.estado='PENDIENTE_PSICOLOGIA' AND a.fechaderivacion IS NOT NULL AND
                """ + CasosActivosPsicologiaService.EXISTE_PARA_ATENCION + """
                ORDER BY a.fecha_creacion,a.codigo
                """, (rs, row) -> {
            JsonNode datos;
            try {
                datos = objectMapper.readTree(rs.getString("datos"));
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new IllegalStateException("Los datos de la atención no son JSON válido.", ex);
            }
            JsonNode persona = datos.path("persona");
            String tipo = persona.path("tipoIdentificacion").asText();
            String numero = persona.path("numeroIdentificacion").asText();
            List<String> nombres = new ArrayList<>();
            for (String campo : List.of("primerNombre", "segundoNombre", "primerApellido", "segundoApellido")) {
                String valor = persona.path(campo).asText(null);
                if (StringUtils.hasText(valor)) nombres.add(valor.trim());
            }
            Long tomador = rs.getObject("idusuariotomadorpsicologia", Long.class);
            return new Recontacto(rs.getString("codigo"), rs.getInt("version"),
                    rs.getTimestamp("fecha_creacion").toInstant(), datos.path("canalIngreso").asText(null),
                    rs.getString("triaje"), datos.path("motivoConsulta").asText(null),
                    datos.path("notaAph").asText(null), new Persona(tipo, numero, String.join(" ", nombres)),
                    candidatos.buscarParaRecontacto(tipo, numero).stream()
                            .map(caso -> resumen(caso, tipo, numero)).toList(),
                    usuario.equals(tomador), rs.getString("tomada_por"));
        });
    }

    private CasoRecontacto resumen(CasosActivosPsicologiaService.CasoActivo caso, String tipo, String documento) {
        List<String> notas = jdbc.query("""
                WITH elegibles AS (
                    SELECT cp.id AS propio,NULL::bigint AS legacy
                    FROM casopsicologia cp JOIN tipoidentificacion ti ON ti.id=cp.idtipoidentificacion
                    WHERE cp.codigo=? AND ti.codigo=? AND upper(cp.numerodocumento)=upper(?)
                    UNION ALL
                    SELECT NULL::bigint,c.id FROM caso c
                    JOIN cita ci ON ci.id=c.idcita JOIN solicitudatencion s ON s.id=ci.idsolicitudatencion
                    LEFT JOIN remision rem ON rem.id=s.idremision
                    JOIN persona p ON p.id=COALESCE(rem.idremitente,s.idsolicitante)
                    JOIN tipoidentificacion ti ON ti.id=p.idtipoidentificacion
                    WHERE c.codigo=? AND ti.codigo=? AND upper(p.numerodocumento)=upper(?)
                )
                SELECT contenido FROM (
                    SELECT pc.datos->'nota'->>'contenido' AS contenido,pc.fecharegistro AS fecha,pc.id
                    FROM primercontactopsicologia pc JOIN elegibles e
                        ON pc.idcasopsicologia=e.propio OR pc.idcaso=e.legacy
                    WHERE pc.datos->'nota'->>'contenido' IS NOT NULL
                    UNION ALL
                    SELECT r.nota,r.fecha,r.id FROM resolucionrecontactopsicologia r JOIN elegibles e
                        ON r.idcasopsicologia=e.propio OR r.idcaso=e.legacy
                    WHERE r.decision='CONTENCION_PUNTUAL'
                ) notas ORDER BY fecha DESC,id DESC LIMIT 1
                """, (rs, row) -> rs.getString("contenido"),
                caso.numeroCaso(), tipo, documento, caso.numeroCaso(), tipo, documento);
        return new CasoRecontacto(caso.numeroCaso(), caso.estado(), caso.fechaApertura(), caso.profesional(),
                caso.numeroSeguimientos(), caso.proximoControl(), notas.isEmpty() ? null : notas.getFirst());
    }

    @Transactional(readOnly = true)
    public DetalleResolucion obtenerResolucion(String codigo, Authentication auth) {
        atenciones.verificarRol(auth, LECTURA);
        AtencionAlmaAph atencion = buscarNovedad(codigo, false);
        List<DetalleResolucion> resultados = jdbc.query("""
                SELECT r.decision,COALESCE(cp.codigo,c.codigo) AS caso,r.nota,r.reabrio,r.fecha,u.nombre
                FROM resolucionrecontactopsicologia r
                JOIN usuario u ON u.id=r.idusuario
                LEFT JOIN casopsicologia cp ON cp.id=r.idcasopsicologia
                LEFT JOIN caso c ON c.id=r.idcaso WHERE r.idatencion=?
                """, (rs, row) -> new DetalleResolucion(codigo, rs.getString("decision"), rs.getString("caso"),
                rs.getString("nota"), rs.getBoolean("reabrio"), rs.getTimestamp("fecha").toInstant(),
                rs.getString("nombre")), atencion.getId());
        if (resultados.isEmpty()) {
            throw new AtencionAlmaException(HttpStatus.NOT_FOUND, "RESOLUCION_NO_ENCONTRADA",
                    "La novedad no tiene una resolución de recontacto.", Map.of());
        }
        return resultados.getFirst();
    }

    @Transactional
    public Resolucion resolver(String codigo, JsonNode request, Authentication auth) {
        atenciones.verificarRol(auth, ESCRITURA);
        ObjectNode datos = atenciones.comprobarObjeto(request, "",
                Set.of("version", "decision", "numeroCaso", "nota", "reabrirCaso", "estadoAtencion"));
        AtencionAlmaAph atencion = buscarNovedad(codigo, true);
        Usuario actor = atenciones.usuario(auth);
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM resolucionrecontactopsicologia WHERE idatencion=?)
                """, Boolean.class, atencion.getId());
        if (Boolean.TRUE.equals(existe)) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "RECONTACTO_YA_RESUELTO",
                    "Este recontacto ya fue resuelto.", Map.of());
        }
        novedades.verificarGestion(atencion, datos.get("version"), actor);
        Map<String, String> errores = new LinkedHashMap<>();
        String decision = texto(datos, "decision");
        String numeroCaso = texto(datos, "numeroCaso");
        String nota = texto(datos, "nota");
        String estado = texto(datos, "estadoAtencion");
        JsonNode reapertura = datos.get("reabrirCaso");
        if (decision == null || !Set.of("CASO_NUEVO", "VINCULAR_A_CASO", "CONTENCION_PUNTUAL").contains(decision))
            errores.put("decision", "Seleccione una decisión válida.");
        if (nota == null || nota.trim().length() < 5 || nota.length() > 1000)
            errores.put("nota", "Ingrese una nota de 5 a 1000 caracteres.");
        if (reapertura != null && !reapertura.isNull() && !reapertura.isBoolean())
            errores.put("reabrirCaso", "Indique si desea reabrir el caso.");
        boolean reabrir = reapertura != null && reapertura.isBoolean() && reapertura.booleanValue();
        if ("CASO_NUEVO".equals(decision)) {
            if (StringUtils.hasText(numeroCaso)) errores.put("numeroCaso", "No indique un caso para caso nuevo.");
            if (reabrir) errores.put("reabrirCaso", "No se reabre un caso al crear uno nuevo.");
        } else if (decision != null && !StringUtils.hasText(numeroCaso)) {
            errores.put("numeroCaso", "Seleccione un caso de la persona.");
        }
        if ("CONTENCION_PUNTUAL".equals(decision)) {
            if (estado == null || !Set.of("CERRADO", "EN_SEGUIMIENTO").contains(estado))
                errores.put("estadoAtencion", "Seleccione CERRADO o EN_SEGUIMIENTO.");
        } else if (estado != null) {
            errores.put("estadoAtencion", "El estado solo se indica para una contención.");
        }
        if (!errores.isEmpty()) throw atenciones.validacion(errores);

        JsonNode persona = atencion.getDatos().path("persona");
        String tipo = persona.path("tipoIdentificacion").asText();
        String documento = persona.path("numeroIdentificacion").asText();
        creacion.bloquearIdentidad(tipo, documento);
        var disponibles = candidatos.buscarParaRecontacto(tipo, documento);
        if (disponibles.isEmpty()) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "RECONTACTO_YA_RESUELTO",
                    "La persona no tiene casos elegibles para este recontacto.", Map.of());
        }
        Instant ahora = Instant.now();
        OffsetDateTime fecha = OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC);
        Long propio = null;
        Caso legacy = null;
        Instant cierreAnterior = null;
        if (!"CASO_NUEVO".equals(decision)) {
            String elegido = numeroCaso.trim();
            List<Long> propios = jdbc.queryForList("""
                    SELECT c.id FROM casopsicologia c JOIN tipoidentificacion ti ON ti.id=c.idtipoidentificacion
                    WHERE c.codigo=? AND ti.codigo=? AND upper(c.numerodocumento)=upper(?) FOR UPDATE OF c
                    """, Long.class, elegido, tipo, documento);
            if (!propios.isEmpty()) propio = propios.getFirst();
            else legacy = casos.findByCodigoForUpdate(elegido).orElseThrow(() ->
                    atenciones.validacion(Map.of("numeroCaso", "El caso no está disponible.")));
            var vigentes = candidatos.buscarParaRecontacto(tipo, documento).stream()
                    .filter(c -> elegido.equals(c.numeroCaso())).toList();
            if (vigentes.size() != 1) {
                throw atenciones.validacion(Map.of("numeroCaso", "Seleccione un caso elegible y único de la persona."));
            }
            boolean cerrado = "CERRADO".equals(vigentes.getFirst().estado());
            if (reabrir && !cerrado)
                throw atenciones.validacion(Map.of("reabrirCaso", "Solo se puede reabrir un caso cerrado."));
            if (cerrado && !reabrir && !"CONTENCION_PUNTUAL".equals(decision))
                throw atenciones.validacion(Map.of("reabrirCaso", "Para vincular el caso cerrado debe reabrirlo."));
            if (propio != null) {
                jdbc.update("""
                        UPDATE casopsicologia SET estado=CASE WHEN ? THEN 'ACTIVO' ELSE estado END,
                            version=version+1,fechaactualizacion=? WHERE id=?
                        """, reabrir, fecha, propio);
            } else if (reabrir) {
                cierreAnterior = legacy.getFechaCierre();
                legacy.setEstadoCaso(estados.findByNombre("Abierto activo").orElseThrow(() ->
                        new IllegalStateException("No está configurado el estado de caso Abierto activo.")));
                legacy.setFechaCierre(null);
                legacy.setUsuarioActualizacion(actor);
                casos.saveAndFlush(legacy);
            }
            if (propio != null) {
                jdbc.update("""
                        INSERT INTO eventocasopsicologia (idcasopsicologia,idusuario,accion,detalle,fecha)
                        VALUES (?,?,?,?,?)
                        """, propio, actor.getId(), reabrir ? "REAPERTURA" : "VINCULACION",
                        reabrir ? "Se reabrió el caso por decisión de Psicología." :
                                "Se vinculó un recontacto por decisión de Psicología.", fecha);
            } else {
                String detalle = reabrir
                        ? "Se reabrió el caso por decisión de Psicología. Fecha de cierre anterior: " + cierreAnterior
                        : "Se vinculó un recontacto por decisión de Psicología.";
                auditoria.registrar(legacy, actor, reabrir ? "REAPERTURA" : "VINCULACION", detalle);
            }
            numeroCaso = elegido;
        }
        jdbc.update("""
                INSERT INTO resolucionrecontactopsicologia
                (idatencion,idcasopsicologia,idcaso,idusuario,decision,nota,reabrio,fechacierreanterior,fecha)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, atencion.getId(), propio, legacy == null ? null : legacy.getId(), actor.getId(), decision,
                nota.trim(), reabrir, cierreAnterior == null ? null : OffsetDateTime.ofInstant(cierreAnterior, ZoneOffset.UTC),
                fecha);
        String estadoFinal = switch (decision) {
            case "CASO_NUEVO" -> "PENDIENTE_PSICOLOGIA";
            case "VINCULAR_A_CASO" -> "EN_SEGUIMIENTO";
            default -> estado;
        };
        atencion.setEstado(estadoFinal);
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        atenciones.registrarEvento(atencion, actor, "RESOLUCION_PSICOLOGIA", ahora);
        log.info("Resolución de recontacto registrada para atención {}", codigo);
        return new Resolucion(codigo, atencion.getVersion(), decision,
                "CASO_NUEVO".equals(decision) ? null : numeroCaso, estadoFinal, reabrir);
    }

    private String texto(ObjectNode datos, String campo) {
        JsonNode valor = datos.get(campo);
        if (valor == null || valor.isNull()) return null;
        if (!valor.isTextual()) throw atenciones.validacion(Map.of(campo, "El valor debe ser texto."));
        return valor.asText();
    }

    private AtencionAlmaAph buscarNovedad(String codigo, boolean bloquear) {
        try {
            return novedades.buscarNovedad(codigo, bloquear);
        } catch (AtencionAlmaException ex) {
            if (!"ATENCION_NO_ENCONTRADA".equals(ex.getCodigo())) throw ex;
            throw new AtencionAlmaException(HttpStatus.NOT_FOUND, "NOVEDAD_NO_ENCONTRADA",
                    "No se encontró una novedad derivada a Psicología.", Map.of());
        }
    }
}
