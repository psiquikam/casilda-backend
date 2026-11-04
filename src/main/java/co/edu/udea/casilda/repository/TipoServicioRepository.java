package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestionar entidades TipoServicio
 */
@Repository
public interface TipoServicioRepository extends JpaRepository<TipoServicio, Integer> {

    /**
     * Busca un registro activo por ID
     */
    Optional<TipoServicio> findByIdAndActivoTrue(Integer id);

    /**
     * Lista los tipos de servicio activos ordenados por nombre
     */
    List<TipoServicio> findByActivoTrueOrderByNombreAsc();
}
