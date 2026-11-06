package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.ReporteCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para gestionar reportes iniciales de caso.
 */
@Repository
public interface ReporteCasoRepository extends JpaRepository<ReporteCaso, Long> {

    /**
     * Busca un reporte por su código público.
     */
    Optional<ReporteCaso> findByCodigo(String codigo);

    /**
     * Comprueba si un código público ya está en uso.
     */
    boolean existsByCodigo(String codigo);
}
