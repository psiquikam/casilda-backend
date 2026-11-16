package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.AtencionAlmaDatos;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.EventoAtencionAlma;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.AtencionAlmaAphRepository;
import co.edu.udea.casilda.repository.EventoAtencionAlmaRepository;
import co.edu.udea.casilda.repository.TipoIdentificacionRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtencionAlmaService {
    private static final Set<String> CAMPOS = Set.of(
            "canalIngreso", "persona", "solicitante", "motivoConsulta", "notaAph", "aceptaPsicologia");
    private static final Set<String> CAMPOS_PERSONA = Set.of("tipoIdentificacion", "numeroIdentificacion",
            "nombres", "apellidos", "telefonoPrincipal", "correoPersonal", "ciudad", "estrato");
    private static final Set<String> CAMPOS_SOLICITANTE = Set.of(
            "esTercero", "nombres", "apellidos", "telefono", "parentesco");
    private static final Set<String> EDITABLES = Set.of("BORRADOR", "EN_GESTION_APH");

    private final AtencionAlmaAphRepository atenciones;
    private final EventoAtencionAlmaRepository eventos;
    private final UsuarioRepository usuarios;
    private final TipoIdentificacionRepository tiposIdentificacion;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Transactional
    public JsonNode crear(JsonNode request, Authentication auth) {
        verificarRol(auth, Set.of("ADMIN", "COORDINADOR", "TECNICO_APH"));
        ObjectNode datos = comprobarObjeto(request, "", CAMPOS);
        validarTipos(datos);
        validarDatos(datos);
        Usuario usuario = usuario(auth);
        Instant ahora = Instant.now();
        AtencionAlmaAph atencion = new AtencionAlmaAph();
        atencion.setCodigo(generarCodigo(ahora));
        atencion.setVersion(1);
        atencion.setEstado("BORRADOR");
        atencion.setTomadaPor(usuario);
        atencion.setDatos(datos.deepCopy());
        atencion.setFechaCreacion(ahora);
        atencion.setFechaActualizacion(ahora);
        actualizarNota(atencion, null, datos, usuario, ahora);
        atenciones.saveAndFlush(atencion);
        registrarEvento(atencion, usuario, "CREACION", ahora);
        log.info("Creada atención APH {}", codigo(atencion));
        return respuesta(atencion);
    }

    @Transactional(readOnly = true)
    public JsonNode obtener(String codigo, Authentication auth) {
        verificarRol(auth, Set.of("ADMIN", "COORDINADOR", "PROFESIONAL", "REVISOR", "TECNICO_APH"));
        AtencionAlmaAph atencion = buscar(codigo, false);
        verificarPropiedad(atencion, auth);
        return respuesta(atencion);
    }

    @Transactional
    public JsonNode actualizar(String codigo, JsonNode request, Authentication auth) {
        verificarRol(auth, Set.of("ADMIN", "COORDINADOR", "TECNICO_APH"));
        ObjectNode patch = comprobarObjeto(request, "", union(CAMPOS, "version"));
        AtencionAlmaAph atencion = buscar(codigo, true);
        verificarPropiedad(atencion, auth);
        verificarVersionYEstado(atencion, patch.get("version"));
        validarTipos(patch);
        JsonNode persona = patch.get("persona");
        if (persona != null && persona.isObject()
                && (persona.has("tipoIdentificacion") || persona.has("numeroIdentificacion"))) {
            throw validacion(Map.of("persona", "El documento no se puede modificar."));
        }
        ObjectNode datos = ((ObjectNode) atencion.getDatos()).deepCopy();
        patch.fields().forEachRemaining(entry -> {
            if ("version".equals(entry.getKey())) {
                return;
            }
            if ("persona".equals(entry.getKey()) && entry.getValue().isObject()) {
                ObjectNode personaActual = (ObjectNode) datos.get("persona");
                entry.getValue().fields().forEachRemaining(campo -> personaActual.set(campo.getKey(), campo.getValue()));
            } else {
                datos.set(entry.getKey(), entry.getValue());
            }
        });
        validarDatos(datos);
        Usuario usuario = usuario(auth);
        Instant ahora = Instant.now();
        actualizarNota(atencion, atencion.getDatos(), datos, usuario, ahora);
        atencion.setDatos(datos);
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        registrarEvento(atencion, usuario, "ACTUALIZACION", ahora);
        log.info("Actualizada atención APH {}", codigo(atencion));
        return respuesta(atencion);
    }

    @Transactional
    public JsonNode registrarTriaje(String codigo, JsonNode request, Authentication auth) {
        verificarRol(auth, Set.of("TECNICO_APH"));
        ObjectNode datos = comprobarObjeto(request, "", Set.of(
                "version", "triaje", "peligroInminente", "justificacionOmision"));
        AtencionAlmaAph atencion = buscar(codigo, true);
        verificarPropiedad(atencion, auth);
        verificarVersionYEstado(atencion, datos.get("version"));
        validarTipo(datos, "triaje", JsonNode::isTextual, "");
        validarTipo(datos, "peligroInminente", JsonNode::isBoolean, "");
        validarTipo(datos, "justificacionOmision", JsonNode::isTextual, "");

        String triaje = datos.path("triaje").asText(null);
        boolean peligro = datos.path("peligroInminente").asBoolean(false);
        String justificacion = datos.path("justificacionOmision").asText(null);
        if (triaje != null && !Set.of("ALTO", "MEDIO", "BAJO").contains(triaje)) {
            throw validacion(Map.of("triaje", "Seleccione un triaje válido."));
        }
        if ((triaje == null && !peligro) || (triaje != null && (peligro || justificacion != null))) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "TRIAJE_REQUERIDO",
                    "Indique el triaje o registre el motivo por el que se omite.", Map.of());
        }
        if (peligro && (justificacion == null || justificacion.trim().length() < 10
                || justificacion.length() > 1000)) {
            throw validacion(Map.of("justificacionOmision", "Ingrese una justificación de 10 a 1000 caracteres."));
        }
        if (!StringUtils.hasText(atencion.getDatos().path("motivoConsulta").asText(null))) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "ATENCION_INCOMPLETA",
                    "Complete el motivo de consulta antes de registrar el triaje.",
                    Map.of("motivoConsulta", "Ingrese el motivo de consulta."));
        }
        Instant ahora = Instant.now();
        boolean cambioEstado = "BORRADOR".equals(atencion.getEstado());
        atencion.setTriaje(triaje);
        atencion.setJustificacionOmisionTriaje(peligro ? justificacion : null);
        atencion.setEstado("EN_GESTION_APH");
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        registrarEvento(atencion, usuario(auth), cambioEstado ? "CAMBIO_ESTADO" : "ACTUALIZACION", ahora);
        log.info("Registrado triaje de atención APH {}", codigo(atencion));
        return respuesta(atencion);
    }

    private void verificarVersionYEstado(AtencionAlmaAph atencion, JsonNode version) {
        if (version == null || !version.isIntegralNumber() || !version.canConvertToInt()
                || version.intValue() != atencion.getVersion()) {
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "CONFLICTO_VERSION",
                    "La atención cambió. Recargue antes de continuar.",
                    Map.of("versionActual", atencion.getVersion()));
        }
        if (!EDITABLES.contains(atencion.getEstado())) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "ATENCION_NO_EDITABLE",
                    "La atención no se puede editar en su estado actual.", Map.of("estado", atencion.getEstado()));
        }
    }

    private void validarTipos(ObjectNode datos) {
        validarTipo(datos, "canalIngreso", JsonNode::isTextual, "");
        validarTipo(datos, "motivoConsulta", JsonNode::isTextual, "");
        validarTipo(datos, "notaAph", JsonNode::isTextual, "");
        validarTipo(datos, "aceptaPsicologia", JsonNode::isBoolean, "");
        JsonNode persona = datos.get("persona");
        if (persona != null && !persona.isNull()) {
            ObjectNode objeto = comprobarObjeto(persona, "persona", CAMPOS_PERSONA);
            CAMPOS_PERSONA.stream().filter(c -> !c.equals("estrato"))
                    .forEach(c -> validarTipo(objeto, c, JsonNode::isTextual, "persona."));
            validarTipo(objeto, "estrato", n -> n.isIntegralNumber() && n.canConvertToInt(), "persona.");
        }
        JsonNode solicitante = datos.get("solicitante");
        if (solicitante != null && !solicitante.isNull()) {
            ObjectNode objeto = comprobarObjeto(solicitante, "solicitante", CAMPOS_SOLICITANTE);
            CAMPOS_SOLICITANTE.stream().filter(c -> !c.equals("esTercero"))
                    .forEach(c -> validarTipo(objeto, c, JsonNode::isTextual, "solicitante."));
            validarTipo(objeto, "esTercero", JsonNode::isBoolean, "solicitante.");
        }
    }

    private void validarDatos(ObjectNode datos) {
        AtencionAlmaDatos dto;
        try {
            dto = objectMapper.treeToValue(datos, AtencionAlmaDatos.class);
        } catch (JsonProcessingException ex) {
            throw validacion(Map.of("request", "Los datos no tienen el formato esperado."));
        }
        Map<String, String> errores = new LinkedHashMap<>();
        for (ConstraintViolation<AtencionAlmaDatos> violation : validator.validate(dto)) {
            errores.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        if (StringUtils.hasText(dto.getCanalIngreso())
                && !Set.of("LLAMADA", "WHATSAPP").contains(dto.getCanalIngreso())) {
            errores.put("canalIngreso", "Seleccione un canal de ingreso válido.");
        }
        if (dto.getPersona() != null) {
            AtencionAlmaDatos.Persona persona = dto.getPersona();
            String tipo = persona.getTipoIdentificacion();
            String numero = persona.getNumeroIdentificacion();
            if (StringUtils.hasText(tipo) && tiposIdentificacion.findByCodigo(tipo)
                    .filter(t -> Boolean.TRUE.equals(t.getActivo())).isEmpty()) {
                errores.put("persona.tipoIdentificacion", "Seleccione un tipo de documento activo.");
            }
            if (StringUtils.hasText(tipo) && StringUtils.hasText(numero)) {
                int maximo = switch (tipo) {
                    case "CC" -> 10;
                    case "TI" -> 11;
                    case "CE" -> 6;
                    case "PA", "PAS" -> 16;
                    default -> 20;
                };
                String patron = Set.of("CE", "PA", "PAS").contains(tipo) ? "[A-Za-z0-9]+" : "[0-9]+";
                if (numero.length() < 4 || numero.length() > maximo || !numero.matches(patron)) {
                    errores.put("persona.numeroIdentificacion", "Revise el número de documento.");
                }
            }
        }
        if (!errores.isEmpty()) {
            throw validacion(errores);
        }
    }

    private ObjectNode comprobarObjeto(JsonNode node, String campo, Set<String> permitidos) {
        if (!(node instanceof ObjectNode objeto)) {
            throw validacion(Map.of(campo.isEmpty() ? "request" : campo, "Se esperaba un objeto."));
        }
        objeto.fieldNames().forEachRemaining(nombre -> {
            if (!permitidos.contains(nombre)) {
                throw validacion(Map.of(campo.isEmpty() ? nombre : campo + "." + nombre, "Campo no permitido."));
            }
        });
        return objeto;
    }

    private void validarTipo(ObjectNode objeto, String campo, java.util.function.Predicate<JsonNode> tipo, String prefijo) {
        JsonNode valor = objeto.get(campo);
        if (valor != null && !valor.isNull() && !tipo.test(valor)) {
            throw validacion(Map.of(prefijo + campo, "El valor no tiene el formato esperado."));
        }
    }

    private Set<String> union(Set<String> valores, String adicional) {
        java.util.HashSet<String> resultado = new java.util.HashSet<>(valores);
        resultado.add(adicional);
        return resultado;
    }

    private AtencionAlmaAph buscar(String codigo, boolean bloquear) {
        if (!codigo.matches("ALMA-\\d{4}-\\d{6}")) {
            throw noEncontrada();
        }
        return (bloquear ? atenciones.findByCodigoForUpdate(codigo) : atenciones.findByCodigo(codigo))
                .orElseThrow(this::noEncontrada);
    }

    private void verificarPropiedad(AtencionAlmaAph atencion, Authentication auth) {
        boolean tecnico = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECNICO_APH"));
        if (tecnico && !atencion.getTomadaPor().getId().equals(usuario(auth).getId())) {
            throw new AtencionAlmaException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                    "No tiene acceso a esta atención.", Map.of());
        }
    }

    private void verificarRol(Authentication auth, Set<String> roles) {
        boolean permitido = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> roles.stream().anyMatch(rol -> a.getAuthority().equals("ROLE_" + rol)));
        if (!permitido) {
            throw new AtencionAlmaException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                    "No tiene acceso a esta operación.", Map.of());
        }
    }

    private Usuario usuario(Authentication auth) {
        return usuarios.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario autenticado"));
    }

    private void actualizarNota(AtencionAlmaAph atencion, JsonNode anterior, JsonNode nuevo,
                                Usuario usuario, Instant ahora) {
        String antes = anterior == null ? null : anterior.path("notaAph").asText(null);
        String despues = nuevo.path("notaAph").asText(null);
        if (!java.util.Objects.equals(antes, despues)) {
            atencion.setFechaNota(StringUtils.hasText(despues) ? ahora : null);
            atencion.setAutorNota(StringUtils.hasText(despues) ? usuario : null);
        }
    }

    private void registrarEvento(AtencionAlmaAph atencion, Usuario usuario, String accion, Instant fecha) {
        EventoAtencionAlma evento = new EventoAtencionAlma();
        evento.setAtencion(atencion);
        evento.setUsuario(usuario);
        evento.setAccion(accion);
        evento.setFecha(fecha);
        eventos.save(evento);
    }

    private ObjectNode respuesta(AtencionAlmaAph atencion) {
        ObjectNode salida = ((ObjectNode) atencion.getDatos()).deepCopy();
        salida.put("idAtencion", codigo(atencion));
        salida.put("version", atencion.getVersion());
        salida.put("estado", atencion.getEstado());
        salida.putNull("origenRemision");
        if (atencion.getTriaje() == null) {
            salida.putNull("triaje");
        } else {
            salida.put("triaje", atencion.getTriaje());
        }
        if (atencion.getJustificacionOmisionTriaje() == null) {
            salida.putNull("omisionTriaje");
        } else {
            ObjectNode omision = salida.putObject("omisionTriaje");
            omision.put("peligroInminente", true);
            omision.put("justificacion", atencion.getJustificacionOmisionTriaje());
        }
        salida.putNull("consentimiento");
        salida.putNull("fechaDerivacion");
        salida.putNull("sla");
        salida.put("tomadaPor", atencion.getTomadaPor().getNombre());
        salida.putNull("cierre");
        salida.putNull("numeroCaso");
        salida.putNull("gestionSimultanea");
        salida.put("fechaCreacion", atencion.getFechaCreacion().toString());
        salida.put("fechaActualizacion", atencion.getFechaActualizacion().toString());
        var notas = salida.putArray("notas");
        if (atencion.getFechaNota() != null) {
            ObjectNode nota = notas.addObject();
            nota.put("idNota", "NOTA-" + atencion.getId());
            nota.put("tipoOrigen", "APH");
            nota.put("contenido", atencion.getDatos().path("notaAph").asText());
            nota.put("fechaRegistro", atencion.getFechaNota().toString());
            nota.put("autor", atencion.getAutorNota().getNombre());
        }
        salida.putArray("derivaciones");
        salida.putArray("rutasExternas");
        return salida;
    }

    private String codigo(AtencionAlmaAph atencion) {
        return atencion.getCodigo();
    }

    private String generarCodigo(Instant fecha) {
        int anio = fecha.atZone(ZoneOffset.UTC).getYear();
        var numeros = jdbc.queryForList("""
                INSERT INTO consecutivoatencionalma (anio, ultimo) VALUES (?, 1)
                ON CONFLICT (anio) DO UPDATE SET ultimo = consecutivoatencionalma.ultimo + 1
                WHERE consecutivoatencionalma.ultimo < 999999
                RETURNING ultimo
                """, Integer.class, anio);
        if (numeros.isEmpty()) {
            throw new IllegalStateException("Se agotó el consecutivo anual de atenciones APH");
        }
        return "ALMA-%04d-%06d".formatted(anio, numeros.get(0));
    }

    private AtencionAlmaException noEncontrada() {
        return new AtencionAlmaException(HttpStatus.NOT_FOUND, "ATENCION_NO_ENCONTRADA",
                "No se encontró la atención.", Map.of());
    }

    private AtencionAlmaException validacion(Map<String, String> detalles) {
        return new AtencionAlmaException(HttpStatus.BAD_REQUEST, "VALIDACION",
                "Revise los datos marcados e intente de nuevo.", detalles);
    }
}
