package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.IntentoContactoPsicologiaResponse;
import co.edu.udea.casilda.dto.response.NotificacionesPsicologiaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.IntentoContactoPsicologia;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.CatalogoAlmaItemRepository;
import co.edu.udea.casilda.repository.IntentoContactoPsicologiaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NovedadPsicologiaService {
    private static final Set<String> ROLES_LECTURA = Set.of("PROFESIONAL", "COORDINADOR", "ADMIN", "REVISOR");
    private static final Set<String> ROLES_ESCRITURA = Set.of("PROFESIONAL", "COORDINADOR", "ADMIN");
    private static final int MAX_INTENTOS = 9;
    private static final int MIN_NOTA = 5;
    private static final int MIN_JUSTIFICACION = 10;

    private final AtencionAlmaService atencionService;
    private final IntentoContactoPsicologiaRepository intentos;
    private final CatalogoAlmaItemRepository catalogos;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate jdbc;
    private final CasosActivosPsicologiaService casosActivos;

    @Transactional(readOnly = true)
    public NotificacionesPsicologiaResponse notificaciones(Authentication auth) {
        atencionService.verificarRol(auth, ROLES_LECTURA);
        Instant ahora = Instant.now();
        String sql = """
                SELECT count(*) AS pendientes,
                       count(*) FILTER (WHERE
                """ + CasosActivosPsicologiaService.EXISTE_PARA_ATENCION + """
                       ) AS recontactos,
                       count(*) FILTER (WHERE a.fechaderivacion +
                           (CASE a.triaje WHEN 'ALTO' THEN 12 WHEN 'MEDIO' THEN 24 ELSE 48 END)
                           * interval '1 hour' <= CAST(:ahora AS timestamptz)) AS vencidos
                FROM atencionalmaaph a
                WHERE a.estado = 'PENDIENTE_PSICOLOGIA' AND a.fechaderivacion IS NOT NULL
                """;
        return jdbc.queryForObject(sql,
                new MapSqlParameterSource("ahora", OffsetDateTime.ofInstant(ahora, java.time.ZoneOffset.UTC)),
                (rs, row) -> new NotificacionesPsicologiaResponse(
                        rs.getLong("pendientes"), rs.getLong("recontactos"), rs.getLong("vencidos"), ahora));
    }

    @Transactional(readOnly = true)
    public JsonNode obtener(String idAtencion, Authentication auth) {
        atencionService.verificarRol(auth, ROLES_LECTURA);
        AtencionAlmaAph atencion = buscarNovedad(idAtencion, false);
        return respuestaPsicologia(atencion, auth);
    }

    @Transactional(readOnly = true)
    public List<IntentoContactoPsicologiaResponse> listarIntentos(String idAtencion, Authentication auth) {
        atencionService.verificarRol(auth, ROLES_LECTURA);
        AtencionAlmaAph atencion = buscarNovedad(idAtencion, false);
        return intentos.findByAtencion_IdOrderByNumeroAsc(atencion.getId()).stream()
                .map(this::respuestaIntento)
                .toList();
    }

    @Transactional
    public JsonNode registrarIntento(String idAtencion, JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, ROLES_ESCRITURA);
        ObjectNode cuerpo = atencionService.comprobarObjeto(request, "",
                Set.of("version", "fechaHora", "resultado", "nota", "justificacion"));
        AtencionAlmaAph atencion = buscarNovedad(idAtencion, true);
        Usuario actor = atencionService.usuario(auth);
        verificarGestion(atencion, cuerpo.get("version"), actor);

        List<IntentoContactoPsicologia> historial = intentos.findByAtencion_IdOrderByNumeroAsc(atencion.getId());
        int numero = historial.size() + 1;
        if (numero > MAX_INTENTOS) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "INTENTOS_AGOTADOS",
                    "Ya se registraron los 9 intentos del protocolo.", Map.of("intentos", historial.size()));
        }

        Map<String, String> errores = validarIntento(cuerpo, numero);
        if (!errores.isEmpty()) {
            throw new AtencionAlmaException(HttpStatus.BAD_REQUEST, "VALIDACION",
                    "Revise los datos del intento de contacto.", errores);
        }

        Instant ahora = Instant.now();
        Instant fechaHora = OffsetDateTime.parse(cuerpo.path("fechaHora").asText()).toInstant();
        String justificacion = cuerpo.path("justificacion").asText(null);
        IntentoContactoPsicologia intento = new IntentoContactoPsicologia();
        intento.setAtencion(atencion);
        intento.setNumero(numero);
        intento.setProtocolo(protocolo(numero));
        intento.setFechaHora(fechaHora);
        intento.setResultado(cuerpo.path("resultado").asText());
        intento.setNota(cuerpo.path("nota").asText().trim());
        intento.setJustificacion(StringUtils.hasText(justificacion) ? justificacion.trim() : null);
        intento.setAutor(actor);
        intentos.saveAndFlush(intento);

        if (numero == MAX_INTENTOS) {
            ObjectNode datos = ((ObjectNode) atencion.getDatos()).deepCopy();
            ObjectNode cierre = datos.putObject("cierre");
            cierre.put("motivo", "NO_CONTACTO");
            cierre.put("observacion", "Cierre automático: se agotaron los 9 intentos de contacto.");
            cierre.put("fecha", ahora.toString());
            atencion.setDatos(datos);
            atencion.setEstado("CERRADO");
        }
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        atencionService.registrarEvento(atencion, actor,
                numero == MAX_INTENTOS ? "CAMBIO_ESTADO" : "INTENTO_PSICOLOGIA", ahora);
        log.info("Registrado intento {}/{} para novedad {}", numero, MAX_INTENTOS, idAtencion);
        return respuestaPsicologia(atencion, auth);
    }

    JsonNode respuestaPsicologia(AtencionAlmaAph atencion, Authentication auth) {
        ObjectNode respuesta = (ObjectNode) atencionService.respuesta(atencion, auth);
        respuesta.remove("notaAph");
        respuesta.remove("solicitante");
        respuesta.remove("aceptaPsicologia");
        respuesta.set("consentimientoAph", respuesta.remove("consentimiento"));
        respuesta.set("notasAph", respuesta.remove("notas"));
        var persona = atencion.getDatos().path("persona");
        var activos = casosActivos.buscar(persona.path("tipoIdentificacion").asText(),
                persona.path("numeroIdentificacion").asText());
        boolean recontacto = "PENDIENTE_PSICOLOGIA".equals(atencion.getEstado()) && !activos.isEmpty();
        respuesta.set("casoActivo", recontacto ? objectMapper.valueToTree(activos.getFirst())
                : objectMapper.nullNode());
        respuesta.put("recontacto", recontacto);
        respuesta.put("tomadaPorMi", atencion.getTomadaPorPsicologia() != null
                && atencion.getTomadaPorPsicologia().getId().equals(atencionService.usuario(auth).getId()));
        if (atencion.getTomadaPorPsicologia() == null) {
            respuesta.putNull("tomadaPor");
        } else {
            respuesta.put("tomadaPor", atencion.getTomadaPorPsicologia().getNombre());
        }
        respuesta.set("intentos", objectMapper.valueToTree(
                intentos.findByAtencion_IdOrderByNumeroAsc(atencion.getId()).stream()
                        .map(this::respuestaIntento)
                        .toList()));
        respuesta.put("intentosRealizados", intentos.countByAtencion_Id(atencion.getId()));
        respuesta.put("gestionSimultanea", false);
        return respuesta;
    }

    AtencionAlmaAph buscarNovedad(String idAtencion, boolean bloquear) {
        AtencionAlmaAph atencion = atencionService.buscar(idAtencion, bloquear);
        if (atencion.getFechaDerivacion() == null) {
            throw new AtencionAlmaException(HttpStatus.NOT_FOUND, "NOVEDAD_NO_ENCONTRADA",
                    "No se encontró una novedad derivada a Psicología.", Map.of());
        }
        return atencion;
    }

    void verificarGestion(AtencionAlmaAph atencion, JsonNode version, Usuario actor) {
        if (!"PENDIENTE_PSICOLOGIA".equals(atencion.getEstado())) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "NOVEDAD_CERRADA",
                    "La novedad ya no está pendiente.", Map.of("estado", atencion.getEstado()));
        }
        if (atencion.getTomadaPorPsicologia() == null
                || !atencion.getTomadaPorPsicologia().getId().equals(actor.getId())) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "NOVEDAD_NO_TOMADA",
                    "Tome la novedad antes de registrar acciones sobre ella.",
                    Map.of("tomadaPor", atencion.getTomadaPorPsicologia() == null
                            ? "" : atencion.getTomadaPorPsicologia().getNombre()));
        }
        if (version == null || !version.isIntegralNumber() || !version.canConvertToInt()
                || version.intValue() != atencion.getVersion()) {
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "CONFLICTO_VERSION",
                    "La novedad cambió desde que la abrió.",
                    Map.of("versionActual", atencion.getVersion()));
        }
    }

    private Map<String, String> validarIntento(ObjectNode cuerpo, int numero) {
        Map<String, String> errores = new LinkedHashMap<>();
        JsonNode resultado = cuerpo.get("resultado");
        if (resultado == null || !resultado.isTextual()
                || catalogos.findByCatalogoAndCodigoAndActivoTrue("resultados-intento",
                resultado.asText()).isEmpty()) {
            errores.put("resultado", "Seleccione un resultado activo del catálogo.");
        }

        JsonNode fecha = cuerpo.get("fechaHora");
        if (fecha == null || !fecha.isTextual()) {
            errores.put("fechaHora", "Indique la fecha y hora del intento.");
        } else {
            try {
                Instant fechaHora = OffsetDateTime.parse(fecha.asText()).toInstant();
                if (fechaHora.isAfter(Instant.now())) {
                    errores.put("fechaHora", "La fecha del intento no puede estar en el futuro.");
                }
            } catch (DateTimeParseException ex) {
                errores.put("fechaHora", "Ingrese una fecha ISO-8601 con zona horaria.");
            }
        }

        JsonNode nota = cuerpo.get("nota");
        if (nota == null || !nota.isTextual() || nota.asText().trim().length() < MIN_NOTA
                || nota.asText().length() > 2000) {
            errores.put("nota", "Escriba una nota de 5 a 2000 caracteres.");
        }

        JsonNode justificacion = cuerpo.get("justificacion");
        if (numero >= 4 && (justificacion == null || !justificacion.isTextual()
                || justificacion.asText().trim().length() < MIN_JUSTIFICACION
                || justificacion.asText().length() > 2000)) {
            errores.put("justificacion", "A partir del cuarto intento se exige una justificación de 10 a 2000 caracteres.");
        }
        return errores;
    }

    private IntentoContactoPsicologiaResponse respuestaIntento(IntentoContactoPsicologia intento) {
        return new IntentoContactoPsicologiaResponse("INT-%06d".formatted(intento.getId()),
                intento.getNumero(), intento.getProtocolo(), intento.getFechaHora(), intento.getResultado(),
                intento.getNota(), intento.getJustificacion(), intento.getAutor().getNombre());
    }

    private String protocolo(int numero) {
        return switch (numero) {
            case 1 -> "PROTOCOLO_1";
            case 2 -> "PROTOCOLO_2";
            case 3 -> "PROTOCOLO_3";
            default -> "ADICIONAL";
        };
    }
}
