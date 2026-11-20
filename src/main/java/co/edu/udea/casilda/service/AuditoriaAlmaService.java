package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.AuditoriaAlmaResponse;
import co.edu.udea.casilda.exception.AtencionAlmaException;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.EventoCaso;
import co.edu.udea.casilda.model.entity.EventoAtencionAlma;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.EventoCasoRepository;
import co.edu.udea.casilda.repository.EventoAtencionAlmaRepository;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditoriaAlmaService {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Set<String> ROLES_AUDITORIA = Set.of("ADMIN", "ALMA_COORDINACION");

    private final AtencionAlmaService atencionService;
    private final EventoAtencionAlmaRepository eventos;
    private final CasoRepository casos;
    private final EventoCasoRepository eventosCaso;

    @Transactional(readOnly = true)
    public List<AuditoriaAlmaResponse> consultar(String tipoContexto, String idContexto,
                                                 Authentication autenticacion) {
        atencionService.verificarRol(autenticacion, ROLES_AUDITORIA);
        if (!StringUtils.hasText(tipoContexto) || !StringUtils.hasText(idContexto)) {
            throw validacion("tipoContexto", "Indique el tipo y el identificador del contexto.");
        }
        String contexto = tipoContexto.trim();
        String id = idContexto.trim();
        if ("ATENCION_APH".equals(contexto)) {
            atencionService.buscar(id, false);
            return eventos.findByAtencionCodigoOrderByFechaAscIdAsc(id).stream()
                    .map(this::respuesta)
                    .toList();
        }
        if ("CASO".equals(contexto)) {
            Caso caso = buscarCaso(id).orElseThrow(() -> new AtencionAlmaException(
                    HttpStatus.NOT_FOUND, "CASO_NO_ENCONTRADO", "No se encontró el caso solicitado.",
                    Map.of("idContexto", id)));
            return eventosCaso.findByCasoIdOrderByFechaAscIdAsc(caso.getId()).stream()
                    .map(this::respuesta)
                    .toList();
        }
        throw validacion("tipoContexto", "Use ATENCION_APH o CASO.");
    }

    private Optional<Caso> buscarCaso(String referencia) {
        Optional<Caso> porCodigo = casos.findByCodigo(referencia);
        if (porCodigo.isPresent()) {
            return porCodigo;
        }
        try {
            return casos.findById(Long.parseLong(referencia));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private AuditoriaAlmaResponse respuesta(EventoAtencionAlma evento) {
        return new AuditoriaAlmaResponse(
                "EVT-%06d".formatted(evento.getId()),
                evento.getFecha().atZone(ZONA).toOffsetDateTime(),
                evento.getUsuario().getNombre(),
                evento.getAccion(),
                evento.getDetalle());
    }

    private AuditoriaAlmaResponse respuesta(EventoCaso evento) {
        return new AuditoriaAlmaResponse(
                "EVT-%06d".formatted(evento.getId()),
                evento.getFecha().atZone(ZONA).toOffsetDateTime(),
                evento.getUsuario().getNombre(),
                evento.getAccion(),
                evento.getDetalle());
    }

    private AtencionAlmaException validacion(String campo, String mensaje) {
        return new AtencionAlmaException(HttpStatus.BAD_REQUEST, "VALIDACION", "Revise el contexto solicitado.",
                Map.of(campo, mensaje));
    }
}
