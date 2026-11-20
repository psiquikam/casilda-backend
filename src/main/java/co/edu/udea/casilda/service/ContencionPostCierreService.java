package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.ContencionPostCierreResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.AtencionAlmaAph;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.ContencionPostCierre;
import co.edu.udea.casilda.model.entity.Persona;
import co.edu.udea.casilda.model.entity.SolicitudAtencion;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.ContencionPostCierreRepository;
import co.edu.udea.casilda.repository.PersonaRepository;
import co.edu.udea.casilda.repository.ProfesionalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContencionPostCierreService {
    private final AtencionAlmaService atencionService;
    private final CasoRepository casos;
    private final PersonaRepository personas;
    private final ProfesionalRepository profesionales;
    private final ContencionPostCierreRepository contenciones;

    @Transactional
    public ContencionPostCierreResponse crear(JsonNode request, Authentication auth) {
        atencionService.verificarRol(auth, Set.of("TECNICO_APH", "PROFESIONAL"));
        boolean esTecnico = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECNICO_APH"));
        if (!esTecnico) {
            Usuario profesional = atencionService.usuario(auth);
            if (profesional.getPersona() == null
                    || !profesionales.existsByIdPersonaAndCargo_NombreIgnoreCase(
                            profesional.getPersona().getId(), "Psicóloga")) {
                throw new AtencionAlmaException(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                        "Solo Psicología puede registrar contenciones post-cierre.", Map.of());
            }
        }
        ObjectNode datos = atencionService.comprobarObjeto(request, "", Set.of("idAtencion", "numeroCaso", "nota"));
        for (String campo : Set.of("idAtencion", "numeroCaso", "nota")) {
            atencionService.validarTipo(datos, campo, JsonNode::isTextual, "");
        }
        String idAtencion = datos.path("idAtencion").asText(null);
        String numeroCaso = datos.path("numeroCaso").asText(null);
        String nota = datos.path("nota").asText(null);
        if (!StringUtils.hasText(idAtencion)) {
            throw atencionService.validacion(Map.of("idAtencion", "Indique la atención APH."));
        }
        if (!StringUtils.hasText(numeroCaso)) {
            throw atencionService.validacion(Map.of("numeroCaso", "Indique el número de caso."));
        }
        if (!StringUtils.hasText(nota) || nota.trim().length() < 10 || nota.length() > 2000) {
            throw atencionService.validacion(Map.of("nota", "Ingrese una nota de 10 a 2000 caracteres."));
        }
        AtencionAlmaAph atencion = atencionService.buscar(idAtencion.trim(), true);
        atencionService.verificarPropiedad(atencion, auth);
        Caso caso = casos.findByCodigoForUpdate(numeroCaso.trim()).orElseThrow(this::casoInvalido);
        JsonNode persona = atencion.getDatos().path("persona");
        String tipo = persona.path("tipoIdentificacion").asText("");
        String numero = persona.path("numeroIdentificacion").asText("");
        Persona personaAph = personas.findByNumeroDocumentoAndTipoIdentificacion_Codigo(numero.trim(), tipo.trim())
                .orElse(null);
        SolicitudAtencion solicitud = caso.getCita().getSolicitudAtencion();
        Persona personaCaso = solicitud.getRemision() == null || solicitud.getRemision().getRemitente() == null
                ? solicitud.getSolicitante() : solicitud.getRemision().getRemitente();
        if (caso.getEstadoCaso() == null || !"Cerrado".equals(caso.getEstadoCaso().getNombre())
                || personaAph == null || personaCaso == null || !personaAph.getId().equals(personaCaso.getId())) {
            throw casoInvalido();
        }
        Usuario autor = atencionService.usuario(auth);
        Instant ahora = Instant.now();
        ContencionPostCierre contencion = new ContencionPostCierre();
        contencion.setAtencion(atencion);
        contencion.setCaso(caso);
        contencion.setAutor(autor);
        contencion.setNota(nota.trim());
        contencion.setFechaRegistro(ahora);
        contenciones.saveAndFlush(contencion);
        atencionService.registrarEvento(atencion, autor, "NOTA", ahora);
        return ContencionPostCierreResponse.desde(contencion);
    }

    private AtencionAlmaException casoInvalido() {
        return new AtencionAlmaException(HttpStatus.UNPROCESSABLE_ENTITY, "PERSONA_SIN_CASO_CERRADO",
                "El caso no está cerrado o no pertenece a la persona de la atención.", Map.of());
    }
}
