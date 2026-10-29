package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.TipoIdentificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoIdentificacionRepository extends JpaRepository<TipoIdentificacion, Integer> {

    Optional<TipoIdentificacion> findByCodigo(String codigo);
}
