package co.edu.udea.casilda.service;

import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.model.entity.EventoCaso;
import co.edu.udea.casilda.model.entity.Usuario;
import co.edu.udea.casilda.repository.EventoCasoRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaCasoService {
    private final EventoCasoRepository eventos;

    @Transactional
    public void registrar(Caso caso, Usuario usuario, String accion, String detalle) {
        EventoCaso evento = new EventoCaso();
        evento.setCaso(caso);
        evento.setUsuario(usuario);
        evento.setAccion(accion);
        evento.setDetalle(detalle);
        evento.setFecha(Instant.now());
        eventos.save(evento);
        log.info("Evento de auditoría {} registrado para caso {}", accion, caso.getId());
    }
}
