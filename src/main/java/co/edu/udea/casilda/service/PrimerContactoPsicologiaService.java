package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.PrimerContactoPsicologiaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.CreacionCasoPsicologiaRepository;
import co.edu.udea.casilda.repository.PersonaRepository;
import co.edu.udea.casilda.repository.TipoIdentificacionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Consolida el primer contacto y su vinculación explícita a un caso. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrimerContactoPsicologiaService {
    private final AtencionAlmaService atencionService;
    private final NovedadPsicologiaService novedades;
    private final ValidacionPrimerContactoPsicologia validacion;
    private final CasosActivosPsicologiaService activos;
    private final CreacionCasoPsicologiaRepository creacion;
    private final PersonaRepository personas;
    private final TipoIdentificacionRepository tipos;
    private final CasoRepository casos;
    private final AuditoriaCasoService auditoriaCaso;
    private final JdbcTemplate jdbc;

    @Transactional
    public PrimerContactoPsicologiaResponse registrar(String idAtencion, JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, Set.of("PROFESIONAL", "COORDINADOR", "ADMIN"));
        AtencionAlmaAph atencion = novedades.buscarNovedad(idAtencion, true);
        Usuario actor = atencionService.usuario(auth);
        novedades.verificarGestion(atencion, request == null ? null : request.get("version"), actor);
        Instant ahora = Instant.now();
        var contenido = validacion.validar(request, (ObjectNode) atencion.getDatos(), ahora);
        ObjectNode personaDatos = contenido.persona();
        String tipo = personaDatos.path("tipoIdentificacion").asText();
        String documento = personaDatos.path("numeroIdentificacion").asText().toUpperCase(Locale.ROOT);
        creacion.bloquearIdentidad(tipo, documento);

        List<CasosActivosPsicologiaService.CasoActivo> casosActivos = activos.buscar(tipo, documento);
        String decision = contenido.datos().path("vincularA").asText(null);
        if (decision != null && !StringUtils.hasText(decision)) {
            throw atencionService.validacion(Map.of("vincularA", "Seleccione un caso activo o CASO_NUEVO."));
        }
        if (decision == null && !casosActivos.isEmpty()) {
            var caso = casosActivos.getFirst();
            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("numeroCaso", caso.numeroCaso());
            detalle.put("estado", caso.estado());
            detalle.put("fechaApertura", caso.fechaApertura());
            detalle.put("profesional", caso.profesional());
            detalle.put("numeroSeguimientos", caso.numeroSeguimientos());
            detalle.put("proximoControl", caso.proximoControl());
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "CASO_ACTIVO_EXISTENTE",
                    "La persona ya tiene un caso activo. Confirme si desea vincularlo o crear uno nuevo.", detalle);
        }
        boolean vinculado = decision != null && !"CASO_NUEVO".equals(decision);
        if (vinculado && casosActivos.stream().noneMatch(c -> c.numeroCaso().equals(decision))) {
            throw atencionService.validacion(Map.of("vincularA", "Seleccione un caso activo de la persona."));
        }

        Long casoPropio = null;
        Caso casoExistente = null;
        String codigo;
        if (vinculado) {
            var propios = jdbc.queryForList("""
                    SELECT c.id FROM casopsicologia c JOIN tipoidentificacion ti ON ti.id=c.idtipoidentificacion
                    WHERE c.codigo=? AND ti.codigo=? AND upper(c.numerodocumento)=?
                      AND c.estado IN ('ABIERTO','ACTIVO','EN_CIERRE') FOR UPDATE OF c
                    """, Long.class, decision, tipo, documento);
            if (!propios.isEmpty()) {
                casoPropio = propios.getFirst();
            } else {
                casoExistente = casos.findByCodigoForUpdate(decision).orElseThrow(() ->
                        atencionService.validacion(Map.of("vincularA", "El caso ya no está disponible.")));
                if (casoExistente.getEstadoCaso() == null
                        || !Set.of("Abierto activo", "Abierto en transición")
                        .contains(casoExistente.getEstadoCaso().getNombre())
                        || activos.buscar(tipo, documento).stream().noneMatch(c -> c.numeroCaso().equals(decision))) {
                    throw atencionService.validacion(Map.of("vincularA", "El caso ya no está activo para la persona."));
                }
            }
            codigo = decision;
        } else {
            codigo = creacion.reservarCodigo(ahora);
        }

        Persona persona = actualizarPersona(tipo, documento, contenido.datos().path("persona"), personaDatos);
        OffsetDateTime fecha = OffsetDateTime.ofInstant(ahora, ZoneOffset.UTC);
        OffsetDateTime control = OffsetDateTime.ofInstant(contenido.proximoControl(), ZoneOffset.UTC);
        if (!vinculado) {
            Integer tipoId = tipos.findByCodigo(tipo).orElseThrow().getId();
            String prioridad = atencion.getTriaje() == null ? "ALTA" : switch (atencion.getTriaje()) {
                case "ALTO" -> "ALTA";
                case "MEDIO" -> "MEDIA";
                case "BAJO" -> "BAJA";
                default -> throw new IllegalStateException("Triaje inválido en la atención");
            };
            casoPropio = jdbc.queryForObject("""
                    INSERT INTO casopsicologia (codigo,version,estado,prioridad,idtipoidentificacion,
                        numerodocumento,idpersona,idprofesional,triajeaph,fechaapertura,fechaactualizacion,proximocontrol)
                    VALUES (?,1,'ABIERTO',?,?,?,?,?,?,?,?,?) RETURNING id
                    """, Long.class, codigo, prioridad, tipoId, documento, persona == null ? null : persona.getId(),
                    actor.getId(), atencion.getTriaje(), fecha, fecha, control);
        } else if (casoPropio != null) {
            jdbc.update("""
                    UPDATE casopsicologia SET version=version+1, fechaactualizacion=?, proximocontrol=?,
                        idpersona=COALESCE(idpersona, ?) WHERE id=?
                    """, fecha, control, persona == null ? null : persona.getId(), casoPropio);
        }

        ObjectNode datosClinicos = contenido.datos().deepCopy();
        datosClinicos.remove(List.of("version", "vincularA", "persona"));
        ObjectNode nota = datosClinicos.putObject("nota");
        nota.put("tipoOrigen", "PSICO");
        nota.put("autor", actor.getNombre());
        nota.put("fechaRegistro", ahora.toString());
        nota.put("contenido", "Primer contacto efectivo. Riesgo valorado: "
                + datosClinicos.path("valoracionRiesgo").path("nivel").asText() + ". "
                + datosClinicos.path("valoracionRiesgo").path("descripcion").asText().trim()
                + " Acuerdos: " + datosClinicos.path("acuerdos").asText().trim());
        jdbc.update("""
                INSERT INTO primercontactopsicologia
                    (idatencion,idcasopsicologia,idcaso,idusuario,fecharegistro,datos)
                VALUES (?,?,?,?,?,CAST(? AS jsonb))
                """, atencion.getId(), casoPropio, casoExistente == null ? null : casoExistente.getId(),
                actor.getId(), fecha, datosClinicos.toString());
        if (casoPropio != null) {
            jdbc.update("""
                    INSERT INTO eventocasopsicologia (idcasopsicologia,idusuario,accion,detalle,fecha)
                    VALUES (?,?,?,?,?)
                    """, casoPropio, actor.getId(), vinculado ? "VINCULACION" : "CREACION",
                    vinculado ? "Se vinculó una atención de Psicología." : "Se creó el caso de Psicología.", fecha);
        } else {
            auditoriaCaso.registrar(casoExistente, actor, "VINCULACION", "Se vinculó una atención de Psicología.");
        }

        ObjectNode datosAtencion = ((ObjectNode) atencion.getDatos()).deepCopy();
        datosAtencion.set("persona", personaDatos);
        atencion.setDatos(datosAtencion);
        atencion.setEstado("ABIERTO_ACTIVO");
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        atencionService.registrarEvento(atencion, actor, "CAMBIO_ESTADO", ahora);
        log.info("Primer contacto registrado para atención {} y caso {}", idAtencion, codigo);
        return new PrimerContactoPsicologiaResponse(codigo, idAtencion, atencion.getVersion(),
                atencion.getEstado(), vinculado);
    }

    private Persona actualizarPersona(String tipo, String documento, JsonNode cambios, ObjectNode datos) {
        Persona persona = personas.findByNumeroDocumentoIgnoreCaseAndTipoIdentificacion_Codigo(documento, tipo).orElse(null);
        if (persona == null) {
            if (!StringUtils.hasText(datos.path("primerNombre").asText(null))
                    || !StringUtils.hasText(datos.path("primerApellido").asText(null))) return null;
            persona = new Persona();
            persona.setNumeroDocumento(documento);
            persona.setTipoIdentificacion(tipos.findByCodigo(tipo).orElseThrow());
            persona.setPrimerNombre(datos.path("primerNombre").asText().trim());
            persona.setPrimerApellido(datos.path("primerApellido").asText().trim());
            if (datos.hasNonNull("segundoNombre")) persona.setSegundoNombre(datos.path("segundoNombre").asText().trim());
            if (datos.hasNonNull("segundoApellido")) persona.setSegundoApellido(datos.path("segundoApellido").asText().trim());
        } else if (cambios.isObject()) {
            if (StringUtils.hasText(cambios.path("primerNombre").asText(null)))
                persona.setPrimerNombre(cambios.path("primerNombre").asText().trim());
            if (StringUtils.hasText(cambios.path("primerApellido").asText(null)))
                persona.setPrimerApellido(cambios.path("primerApellido").asText().trim());
            if (StringUtils.hasText(cambios.path("segundoNombre").asText(null)))
                persona.setSegundoNombre(cambios.path("segundoNombre").asText().trim());
            if (StringUtils.hasText(cambios.path("segundoApellido").asText(null)))
                persona.setSegundoApellido(cambios.path("segundoApellido").asText().trim());
        }
        return personas.saveAndFlush(persona);
    }
}
