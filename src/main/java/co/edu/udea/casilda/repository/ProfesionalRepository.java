package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.Profesional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestionar entidades Profesional
 */
@Repository
public interface ProfesionalRepository extends JpaRepository<Profesional, Long> {
    /** Verifica el cargo individual del profesional, sin inferirlo desde su grupo. */
    boolean existsByIdPersonaAndCargo_NombreIgnoreCase(Long idPersona, String nombreCargo);
}
