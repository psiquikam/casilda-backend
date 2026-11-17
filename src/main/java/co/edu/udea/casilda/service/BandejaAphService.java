package co.edu.udea.casilda.service;

import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.AtencionAlmaAphRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BandejaAphService {
    private static final Set<String> CAMPOS = Set.of("origenRemision", "referenciaOrigen", "persona",
            "solicitante", "motivo", "peligroInminente", "consentimiento");
    private static final Map<String, Set<String>> ROLES_ORIGEN = Map.of(
            "EA", Set.of("ADMIN", "EA_COORDINACION", "PROFESIONAL", "COORDINADOR"),
            "SPYB", Set.of("ADMIN", "SPB_ANALISTA"),
            "UAD", Set.of("ADMIN", "UAD_COORDINACION", "UAD_EQUIPO_3", "UAD_EQUIPO_4"));
    private static final Set<String> ESTADOS = Set.of("BORRADOR", "EN_GESTION_APH",
            "PENDIENTE_PSICOLOGIA", "ABIERTO_ACTIVO", "EN_SEGUIMIENTO", "CERRADO");

    private final AtencionAlmaService atencionService;
    private final AtencionAlmaAphRepository atenciones;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Transactional
    public JsonNode remitir(JsonNode request, Authentication auth) {
        ObjectNode entrada = atencionService.comprobarObjeto(request, "", CAMPOS);
        for (String campo : List.of("origenRemision", "referenciaOrigen", "motivo")) {
            atencionService.validarTipo(entrada, campo, JsonNode::isTextual, "");
        }
        atencionService.validarTipo(entrada, "peligroInminente", JsonNode::isBoolean, "");
        String origen = entrada.path("origenRemision").asText(null);
        if (origen == null || !ROLES_ORIGEN.containsKey(origen)) {
            throw atencionService.validacion(Map.of("origenRemision", "Seleccione un origen válido."));
        }
        atencionService.verificarRol(auth, ROLES_ORIGEN.get(origen));
        String motivo = entrada.path("motivo").asText(null);
        if (!StringUtils.hasText(motivo) || motivo.trim().length() < 10 || motivo.length() > 2000) {
            throw atencionService.validacion(Map.of("motivo", "Ingrese un motivo de 10 a 2000 caracteres."));
        }
        String referencia = entrada.path("referenciaOrigen").asText(null);
        if (referencia != null && referencia.length() > 50) {
            throw atencionService.validacion(Map.of("referenciaOrigen", "Ingrese máximo 50 caracteres."));
        }
        boolean peligro = entrada.path("peligroInminente").asBoolean(false);
        Instant ahora = Instant.now();
        JsonNode consentimiento = entrada.get("consentimiento");
        if (!peligro || (consentimiento != null && !consentimiento.isNull())) {
            atencionService.validarConsentimiento(consentimiento, ahora);
        }
        ObjectNode datos = objectMapper.createObjectNode();
        datos.put("canalIngreso", "REMISION_" + origen);
        datos.set("persona", entrada.path("persona"));
        if (entrada.has("solicitante")) {
            datos.set("solicitante", entrada.get("solicitante"));
        }
        datos.put("motivoConsulta", motivo);
        atencionService.validarTipos(datos);
        atencionService.validarDatos(datos, true);

        ObjectNode evidencia = objectMapper.createObjectNode();
        evidencia.put("origen", origen);
        evidencia.put("peligroInminente", peligro);
        if (referencia != null) {
            evidencia.put("referenciaOrigen", referencia);
        }
        if (consentimiento != null && !consentimiento.isNull()) {
            evidencia.set("consentimiento", consentimiento.deepCopy());
            evidencia.put("fechaRecepcionConsentimiento", ahora.toString());
        }
        Usuario usuario = atencionService.usuario(auth);
        AtencionAlmaAph atencion = new AtencionAlmaAph();
        atencion.setCodigo(atencionService.generarCodigo(ahora));
        atencion.setEstado("PENDIENTE_APH");
        atencion.setVersion(1);
        atencion.setDatos(datos);
        atencion.setDatosRemision(evidencia);
        atencion.setFechaCreacion(ahora);
        atencion.setFechaActualizacion(ahora);
        atenciones.saveAndFlush(atencion);
        atencionService.registrarEvento(atencion, usuario, "CREACION", ahora);
        log.info("Remisión APH {} recibida de {}", atencion.getCodigo(), origen);
        return atencionService.respuesta(atencion, auth);
    }

    @Transactional
    public JsonNode tomar(String codigo, JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, Set.of("TECNICO_APH"));
        ObjectNode datos = atencionService.comprobarObjeto(request, "", Set.of("version"));
        AtencionAlmaAph atencion = atencionService.buscar(codigo, true);
        if (!"PENDIENTE_APH".equals(atencion.getEstado()) || atencion.getTomadaPor() != null) {
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "REMISION_YA_TOMADA",
                    "La remisión ya fue tomada por otro técnico.",
                    Map.of("tomadaEn", atencion.getFechaActualizacion().toString()));
        }
        JsonNode version = datos.get("version");
        if (version == null || !version.isIntegralNumber() || !version.canConvertToInt()
                || version.intValue() != atencion.getVersion()) {
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "CONFLICTO_VERSION",
                    "La atención cambió. Recargue antes de continuar.",
                    Map.of("versionActual", atencion.getVersion()));
        }
        Usuario usuario = atencionService.usuario(auth);
        Instant ahora = Instant.now();
        atencion.setTomadaPor(usuario);
        atencion.setEstado("EN_GESTION_APH");
        atencion.setVersion(atencion.getVersion() + 1);
        atencion.setFechaActualizacion(ahora);
        atencionService.registrarEvento(atencion, usuario, "CAMBIO_ESTADO", ahora);
        log.info("Tomada remisión APH {}", codigo);
        return atencionService.respuesta(atencion, auth);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listar(String vista, String origen, String estado, int page, int size,
                                      Authentication auth) {
        atencionService.verificarRol(auth, Set.of("ADMIN", "COORDINADOR", "REVISOR", "TECNICO_APH"));
        if (!Set.of("remisiones", "mias").contains(vista) || page < 0 || size < 1 || size > 100
                || (origen != null && (!"remisiones".equals(vista) || !ROLES_ORIGEN.containsKey(origen)))
                || (estado != null && (!"mias".equals(vista) || !ESTADOS.contains(estado)))) {
            throw atencionService.validacion(Map.of("filtros", "Seleccione filtros y paginación válidos."));
        }
        String filtro = "remisiones".equals(vista)
                ? "a.estado = 'PENDIENTE_APH' AND a.datos->>'canalIngreso' LIKE 'REMISION_%' "
                    + "AND (:origen = '' OR a.datos->>'canalIngreso' = :canal)"
                : "a.estado <> 'PENDIENTE_APH' AND a.idusuariotomador = :usuario "
                    + "AND (:estado = '' OR a.estado = :estado)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("origen", origen == null ? "" : origen)
                .addValue("canal", origen == null ? "" : "REMISION_" + origen)
                .addValue("estado", estado == null ? "" : estado)
                .addValue("usuario", atencionService.usuario(auth).getId())
                .addValue("limit", size).addValue("offset", (long) page * size);
        Long total = jdbc.queryForObject("SELECT count(*) FROM atencionalmaaph a WHERE " + filtro, params, Long.class);
        String orden = "remisiones".equals(vista) ? "a.fecha_creacion ASC, a.id ASC"
                : "a.fecha_actualizacion DESC, a.id DESC";
        List<ObjectNode> items = jdbc.query("""
                SELECT a.codigo FROM atencionalmaaph a WHERE
                """ + filtro + " ORDER BY " + orden + " LIMIT :limit OFFSET :offset",
                params, (rs, row) -> {
                    AtencionAlmaAph atencion = atenciones.findByCodigo(rs.getString("codigo")).orElseThrow();
                    ObjectNode salida = atencionService.respuesta(atencion, auth);
                    ObjectNode item = objectMapper.createObjectNode();
                    for (String campo : List.of("idAtencion", "version", "canalIngreso", "origenRemision",
                            "estado", "tomadaPor", "triaje", "sla")) {
                        item.set(campo, salida.path(campo));
                    }
                    ObjectNode persona = item.putObject("persona");
                    JsonNode origenPersona = salida.path("persona");
                    persona.set("tipoIdentificacion", origenPersona.path("tipoIdentificacion"));
                    persona.set("numeroIdentificacion", origenPersona.path("numeroIdentificacion"));
                    persona.put("nombreCompleto", (origenPersona.path("nombres").asText("") + " "
                            + origenPersona.path("apellidos").asText("")).trim());
                    item.set("resumen", salida.path("motivoConsulta"));
                    if (atencion.getDatosRemision() == null) {
                        item.putNull("fechaRemision");
                    } else {
                        item.put("fechaRemision", atencion.getFechaCreacion().toString());
                    }
                    return item;
                });
        return Map.of("items", items, "page", page, "size", size, "total", total);
    }
}
