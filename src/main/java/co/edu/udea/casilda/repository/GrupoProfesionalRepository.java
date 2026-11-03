package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.GrupoProfesional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestionar entidades GrupoProfesional
 */
@Repository
public interface GrupoProfesionalRepository extends JpaRepository<GrupoProfesional, Integer> {

    /**
     * Busca un registro activo por ID
     */
    Optional<GrupoProfesional> findByIdAndActivoTrue(Integer id);

    /**
     * Lista los grupos profesionales activos ordenados por nombre
     */
    List<GrupoProfesional> findByActivoTrueOrderByNombreAsc();
}
