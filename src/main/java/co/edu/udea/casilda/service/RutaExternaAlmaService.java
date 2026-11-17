package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.RutaExternaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.RutaExternaAlma;
import co.edu.udea.casilda.repository.CatalogoAlmaItemRepository;
import co.edu.udea.casilda.repository.RutaExternaAlmaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RutaExternaAlmaService {
    private static final Set<String> ROLES = Set.of("TECNICO_APH", "ADMIN");
    private static final Set<String> ESTADOS = Set.of("ACTIVADA", "EN_SEGUIMIENTO", "CERRADA");
    private final AtencionAlmaService atencionService;
    private final RutaExternaAlmaRepository rutas;
    private final CatalogoAlmaItemRepository catalogos;

    @Transactional
    public RutaExternaAlmaResponse crear(JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, ROLES);
        ObjectNode datos = atencionService.comprobarObjeto(request, "", Set.of("tipoContexto", "idContexto",
                "entidad", "motivo", "fechaActivacion", "peligroInminente", "consentimiento"));
        for (String campo : List.of("tipoContexto", "idContexto", "entidad", "motivo", "fechaActivacion")) {
            atencionService.validarTipo(datos, campo, JsonNode::isTextual, "");
        }
        atencionService.validarTipo(datos, "peligroInminente", JsonNode::isBoolean, "");
        AtencionAlmaAph atencion = contexto(datos.path("tipoContexto").asText(null),
                datos.path("idContexto").asText(null), auth, true);
        verificarAbierta(atencion);
        String entidad = datos.path("entidad").asText(null);
        if (!StringUtils.hasText(entidad)
                || catalogos.findByCatalogoAndCodigoAndActivoTrue("rutas-externas", entidad).isEmpty()) {
            throw atencionService.validacion(Map.of("entidad", "Seleccione una entidad activa."));
        }
        String motivo = datos.path("motivo").asText(null);
        if (!StringUtils.hasText(motivo) || motivo.trim().length() < 5 || motivo.length() > 500) {
            throw atencionService.validacion(Map.of("motivo", "Ingrese un motivo de 5 a 500 caracteres."));
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(datos.path("fechaActivacion").asText(""));
        } catch (DateTimeParseException ex) {
            throw atencionService.validacion(Map.of("fechaActivacion", "Ingrese una fecha válida en formato AAAA-MM-DD."));
        }
        boolean peligro = datos.path("peligroInminente").asBoolean(false);
        if (peligro && !"LINEA_123".equals(entidad)) {
            throw atencionService.validacion(Map.of("peligroInminente", "La excepción solo aplica a Línea 123."));
        }
        Instant ahora = Instant.now();
        JsonNode consentimiento = datos.get("consentimiento");
        if (!peligro || (consentimiento != null && !consentimiento.isNull())) {
            atencionService.validarConsentimiento(consentimiento, ahora, false);
        }
        RutaExternaAlma ruta = new RutaExternaAlma();
        ruta.setAtencion(atencion);
        ruta.setVersion(1);
        ruta.setEntidad(entidad);
        ruta.setMotivo(motivo);
        ruta.setFechaActivacion(fecha);
        ruta.setEstado("ACTIVADA");
        ruta.setPeligroInminente(peligro);
        ruta.setConsentimiento(consentimiento == null || consentimiento.isNull() ? null : consentimiento.deepCopy());
        ruta.setFechaRegistro(ahora);
        rutas.saveAndFlush(ruta);
        atencionService.registrarEvento(atencion, atencionService.usuario(auth), "DERIVACION", ahora);
        log.info("Creada ruta externa RUT-{} para atención {}", ruta.getId(), atencion.getCodigo());
        return RutaExternaAlmaResponse.desde(ruta);
    }

    @Transactional(readOnly = true)
    public List<RutaExternaAlmaResponse> listar(String tipoContexto, String idContexto, Authentication auth) {
        atencionService.verificarRol(auth, ROLES);
        AtencionAlmaAph atencion = contexto(tipoContexto, idContexto, auth, false);
        return rutas.findByAtencionIdOrderByIdAsc(atencion.getId()).stream().map(RutaExternaAlmaResponse::desde).toList();
    }

    @Transactional
    public RutaExternaAlmaResponse actualizar(String codigo, JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, ROLES);
        ObjectNode datos = atencionService.comprobarObjeto(request, "", Set.of("version", "estado", "resultado"));
        long id = idRuta(codigo);
        String codigoAtencion = rutas.findCodigoAtencionById(id).orElseThrow(this::noEncontrada);
        AtencionAlmaAph atencion = contexto("ATENCION_APH", codigoAtencion, auth, true);
        RutaExternaAlma ruta = rutas.findByIdForUpdate(id).orElseThrow(this::noEncontrada);
        JsonNode version = datos.get("version");
        if (version == null || !version.isIntegralNumber() || !version.canConvertToInt()
                || version.intValue() != ruta.getVersion()) {
            throw new AtencionAlmaException(HttpStatus.CONFLICT, "CONFLICTO_VERSION",
                    "La ruta cambió. Recargue antes de continuar.", Map.of("versionActual", ruta.getVersion()));
        }
        verificarAbierta(atencion);
        if ("CERRADA".equals(ruta.getEstado())) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "ATENCION_NO_EDITABLE",
                    "La ruta cerrada no se puede modificar.", Map.of("estado", ruta.getEstado()));
        }
        atencionService.validarTipo(datos, "estado", JsonNode::isTextual, "");
        atencionService.validarTipo(datos, "resultado", JsonNode::isTextual, "");
        String estado = datos.path("estado").asText(null);
        if (estado == null || !ESTADOS.contains(estado)) {
            throw atencionService.validacion(Map.of("estado", "Seleccione un estado válido."));
        }
        String resultado = datos.path("resultado").asText(null);
        if (("CERRADA".equals(estado) && !StringUtils.hasText(resultado))
                || (resultado != null && (!StringUtils.hasText(resultado)
                || catalogos.findByCatalogoAndCodigoAndActivoTrue("resultados-ruta-externa", resultado).isEmpty()))) {
            throw atencionService.validacion(Map.of("resultado", "Seleccione un resultado válido."));
        }
        ruta.setEstado(estado);
        ruta.setResultado(resultado);
        ruta.setVersion(ruta.getVersion() + 1);
        Instant ahora = Instant.now();
        atencionService.registrarEvento(atencion, atencionService.usuario(auth), "ACTUALIZACION", ahora);
        log.info("Actualizada ruta externa {} para atención {}", codigo, atencion.getCodigo());
        return RutaExternaAlmaResponse.desde(ruta);
    }

    private AtencionAlmaAph contexto(String tipo, String codigo, Authentication auth, boolean bloquear) {
        if (!"ATENCION_APH".equals(tipo)) {
            throw atencionService.validacion(Map.of("tipoContexto", "Seleccione una atención APH."));
        }
        if (!StringUtils.hasText(codigo)) {
            throw atencionService.validacion(Map.of("idContexto", "Indique la atención."));
        }
        AtencionAlmaAph atencion = atencionService.buscar(codigo, bloquear);
        if (!atencionService.puedeConsultarRutas(atencion, auth)) {
            throw new AtencionAlmaException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                    "No tiene acceso a esta atención.", Map.of());
        }
        return atencion;
    }

    private void verificarAbierta(AtencionAlmaAph atencion) {
        if ("CERRADO".equals(atencion.getEstado()) || "PENDIENTE_APH".equals(atencion.getEstado())) {
            throw new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "ATENCION_NO_EDITABLE",
                    "La atención no admite cambios en su estado actual.", Map.of("estado", atencion.getEstado()));
        }
    }

    private long idRuta(String codigo) {
        if (!codigo.matches("RUT-\\d{4,19}")) {
            throw noEncontrada();
        }
        try {
            return Long.parseLong(codigo.substring(4));
        } catch (NumberFormatException ex) {
            throw noEncontrada();
        }
    }

    private AtencionAlmaException noEncontrada() {
        return new AtencionAlmaException(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA",
                "No se encontró la ruta externa.", Map.of());
    }
}
