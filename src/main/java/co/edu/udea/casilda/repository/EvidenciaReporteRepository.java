package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.EvidenciaReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para gestionar referencias de evidencias de reportes.
 */
@Repository
public interface EvidenciaReporteRepository extends JpaRepository<EvidenciaReporte, Long> {

    /**
     * Cuenta las evidencias persistidas para un reporte identificado por su código.
     */
    long countByReporteCasoCodigo(String codigo);
}
