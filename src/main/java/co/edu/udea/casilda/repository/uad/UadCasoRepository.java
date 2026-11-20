package co.edu.udea.casilda.repository.uad;

import co.edu.udea.casilda.model.entity.uad.UadCaso;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface UadCasoRepository extends JpaRepository<UadCaso, UUID> {

    boolean existsByCasoId(String casoId);

    /** Bandeja activa (UAD-CAS-12): gestión activa y equipo responsable en la fase actual. */
    Page<UadCaso> findByEstadoGestionAndEquipoResponsableIn(EstadoGestionUad estado, Collection<EquipoUad> equipos, Pageable pageable);

    Page<UadCaso> findByEstadoGestionAndEquipoResponsableInAndFaseActual(
            EstadoGestionUad estado, Collection<EquipoUad> equipos, FaseUad fase, Pageable pageable);
}
