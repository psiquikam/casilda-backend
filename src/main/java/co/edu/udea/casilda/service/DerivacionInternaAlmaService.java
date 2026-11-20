package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.DerivacionInternaAlmaResponse;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.DerivacionInternaAlma;
import co.edu.udea.casilda.repository.CatalogoAlmaItemRepository;
import co.edu.udea.casilda.repository.DerivacionInternaAlmaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class DerivacionInternaAlmaService {
    private static final Set<String> ROLES = Set.of("TECNICO_APH", "ADMIN");
    private final AtencionAlmaService atencionService;
    private final DerivacionInternaAlmaRepository derivaciones;
    private final CatalogoAlmaItemRepository catalogos;

    @Transactional
    public DerivacionInternaAlmaResponse crear(JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, ROLES);
        ObjectNode datos = atencionService.comprobarObjeto(request, "", Set.of("tipoContexto", "idContexto",
                "destino", "motivo", "peligroInminente", "consentimiento"));
        for (String campo : List.of("tipoContexto", "idContexto", "destino", "motivo")) {
            atencionService.validarTipo(datos, campo, JsonNode::isTextual, "");
        }
        atencionService.validarTipo(datos, "peligroInminente", JsonNode::isBoolean, "");
        AtencionAlmaAph atencion = atencionService.contextoAphPropietario(
                datos.path("tipoContexto").asText(null), datos.path("idContexto").asText(null), auth, true);
        atencionService.verificarAbiertaParaRutas(atencion);
        String destino = datos.path("destino").asText(null);
        if (!StringUtils.hasText(destino)
                || catalogos.findByCatalogoAndCodigoAndActivoTrue("destinos-internos", destino).isEmpty()) {
            throw atencionService.validacion(Map.of("destino", "Seleccione un destino activo."));
        }
        String motivo = datos.path("motivo").asText(null);
        if (!StringUtils.hasText(motivo) || motivo.trim().length() < 5 || motivo.length() > 500) {
            throw atencionService.validacion(Map.of("motivo", "Ingrese un motivo de 5 a 500 caracteres."));
        }
        boolean peligro = datos.path("peligroInminente").asBoolean(false);
        if (peligro && !"SPYB".equals(destino)) {
            throw atencionService.validacion(Map.of("peligroInminente", "La excepción solo aplica a SPyB."));
        }
        Instant ahora = Instant.now();
        JsonNode consentimiento = datos.get("consentimiento");
        if (!peligro || (consentimiento != null && !consentimiento.isNull())) {
            atencionService.validarConsentimiento(consentimiento, ahora, false);
        }
        DerivacionInternaAlma derivacion = new DerivacionInternaAlma();
        derivacion.setAtencion(atencion);
        derivacion.setDestino(destino);
        derivacion.setMotivo(motivo);
        derivacion.setPeligroInminente(peligro);
        derivacion.setConsentimiento(consentimiento == null || consentimiento.isNull() ? null : consentimiento.deepCopy());
        derivacion.setFechaRegistro(ahora);
        derivaciones.saveAndFlush(derivacion);
        atencionService.registrarEvento(atencion, atencionService.usuario(auth), "DERIVACION", ahora);
        log.info("Registrada derivación interna DER-{} para atención {}", derivacion.getId(), atencion.getCodigo());
        return DerivacionInternaAlmaResponse.desde(derivacion);
    }

    @Transactional(readOnly = true)
    public List<DerivacionInternaAlmaResponse> listar(String tipoContexto, String idContexto, Authentication auth) {
        atencionService.verificarRol(auth, ROLES);
        AtencionAlmaAph atencion = atencionService.contextoAphPropietario(tipoContexto, idContexto, auth, false);
        return derivaciones.findByAtencionIdOrderByIdAsc(atencion.getId()).stream()
                .map(DerivacionInternaAlmaResponse::desde).toList();
    }
}
