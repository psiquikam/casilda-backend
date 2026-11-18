package co.edu.udea.casilda.repository.uad;

import co.edu.udea.casilda.model.entity.uad.UadNoticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UadNoticiaRepository extends JpaRepository<UadNoticia, UUID>, JpaSpecificationExecutor<UadNoticia> {

    Optional<UadNoticia> findByReporteCasoId(Long reporteCasoId);

    /** Noticias admitidas cuyo reporte de origen tiene el documento dado (consulta de casos asociados). */
    List<UadNoticia> findByReporteCasoNumeroDocumentoAndCasoIdIsNotNull(String numeroDocumento);

    /** Noticias admitidas en estas gestiones UAD (consecutivos y evidencias del caso). */
    List<UadNoticia> findByCasoUadIdInOrderByFechaRecepcionAsc(Collection<UUID> casoUadIds);
}
