package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Integer> {

    /**
     * Busca un campus activo por su identificador.
     */
    Optional<Campus> findByIdAndActivoTrue(Integer id);
}
