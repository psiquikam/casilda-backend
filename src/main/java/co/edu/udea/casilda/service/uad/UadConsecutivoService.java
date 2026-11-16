package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.model.entity.uad.UadConsecutivo;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.repository.uad.UadConsecutivoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.ZoneId;

/**
 * Consecutivos {@code NDI-AAAA-NNNNNN} y {@code NDA-AAAA-NNNNNN} con secuencias separadas por tipo y año
 * (UAD-ID-02, UAD-NOT-02). La fila del contador se crea con {@code INSERT ... ON CONFLICT DO NOTHING} y
 * se lee con bloqueo pesimista: dos radicaciones simultáneas nunca comparten número.
 */
@Service
@RequiredArgsConstructor
public class UadConsecutivoService {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final UadConsecutivoRepository repository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguiente(final TipoNoticia tipo) {
        final int anio = Year.now(BOGOTA).getValue();
        final String clave = tipo.name() + "-" + anio;
        entityManager.createNativeQuery("INSERT INTO uad_consecutivo (clave, ultimo) VALUES (:clave, 0) ON CONFLICT (clave) DO NOTHING")
                .setParameter("clave", clave)
                .executeUpdate();
        final UadConsecutivo contador = repository.findForUpdate(clave)
                .orElseThrow(() -> new IllegalStateException("No se pudo reservar el consecutivo " + clave));
        contador.setUltimo(contador.getUltimo() + 1);
        return "%s-%d-%06d".formatted(tipo.name(), anio, contador.getUltimo());
    }
}
