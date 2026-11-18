package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.request.FechaCierreCasoRequest;
import co.edu.udea.casilda.dto.response.FechaCierreCasoResponse;
import co.edu.udea.casilda.exception.ResourceNotFoundException;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.CasoRepository;
import co.edu.udea.casilda.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FechaCierreCasoService {
    private final CasoRepository casos;
    private final UsuarioRepository usuarios;
    private final AuditoriaCasoService auditoriaCasoService;

    @Transactional
    public FechaCierreCasoResponse actualizar(Long solicitudId, Long casoId, FechaCierreCasoRequest request,
                                             Authentication auth) {
        Caso caso = casos.findByIdForUpdate(casoId)
                .orElseThrow(() -> new ResourceNotFoundException("Caso no encontrado con ID: " + casoId));
        if (!solicitudId.equals(caso.getCita().getSolicitudAtencion().getId())) {
            throw new ResourceNotFoundException("El caso no pertenece a la solicitud indicada.");
        }
        if (caso.getEstadoCaso() == null || !"Cerrado".equalsIgnoreCase(caso.getEstadoCaso().getNombre())) {
            throw new IllegalArgumentException("La fecha de cierre solo se puede editar en un caso cerrado.");
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new IllegalStateException("No se encontró el usuario autenticado"));
        caso.setFechaCierre(request.getFechaCierre());
        caso.setUsuarioActualizacion(usuario);
        caso.setFechaActualizacion(LocalDateTime.now());
        auditoriaCasoService.registrar(caso, usuario, "ACTUALIZACION", "Se actualizó la fecha de cierre del caso.");
        log.info("Actualizada fecha de cierre del caso {}", casoId);
        return new FechaCierreCasoResponse(caso.getId(), caso.getFechaCierre());
    }
}
