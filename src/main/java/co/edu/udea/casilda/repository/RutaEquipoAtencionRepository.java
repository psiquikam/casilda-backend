package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.RutaEquipoAtencion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutaEquipoAtencionRepository extends JpaRepository<RutaEquipoAtencion, Integer> {
    List<RutaEquipoAtencion> findByTipoAndRutaActivoTrueOrderByRutaIdAsc(String tipo);
}
