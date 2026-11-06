package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.RutaActivacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar entidades RutaActivacion
 */
@Repository
public interface RutaActivacionRepository extends JpaRepository<RutaActivacion, Integer> {

    /**
     * Busca una ruta activa por su identificador.
     */
    Optional<RutaActivacion> findByIdAndActivoTrue(Integer id);
}
