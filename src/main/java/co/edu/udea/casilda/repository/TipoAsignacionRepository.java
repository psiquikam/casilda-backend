package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.TipoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestionar entidades TipoAsignacion
 */
@Repository
public interface TipoAsignacionRepository extends JpaRepository<TipoAsignacion, Integer> {

    /**
     * Busca un registro activo por ID
     */
    Optional<TipoAsignacion> findByIdAndActivoTrue(Integer id);

    /**
     * Lista los tipos de asignación activos ordenados por nombre
     */
    List<TipoAsignacion> findByActivoTrueOrderByNombreAsc();
}
