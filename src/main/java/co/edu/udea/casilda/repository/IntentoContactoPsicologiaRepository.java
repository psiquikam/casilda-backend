package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.IntentoContactoPsicologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntentoContactoPsicologiaRepository extends JpaRepository<IntentoContactoPsicologia, Long> {
    /** Lista los intentos de contacto de una novedad en orden de protocolo. */
    List<IntentoContactoPsicologia> findByAtencion_IdOrderByNumeroAsc(Long atencionId);

    /** Cuenta los intentos registrados para una novedad. */
    long countByAtencion_Id(Long atencionId);
}
