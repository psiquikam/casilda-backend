package co.edu.udea.casilda.repository.uad;

import co.edu.udea.casilda.model.entity.uad.UadEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Solo lectura e inserción: el historial UAD es append-only (no se exponen borrados ni actualizaciones). */
public interface UadEventoRepository extends JpaRepository<UadEvento, Long> {

    List<UadEvento> findByNoticiaIdOrderByFechaAscIdAsc(UUID noticiaId);
}
