package co.edu.udea.casilda.repository;

import co.edu.udea.casilda.model.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    List<Endpoint> findByHttpMethodAndActivoTrue(final String httpMethod);

    List<Endpoint> findByActivoTrue();

    Optional<Endpoint> findByPathAndHttpMethod(final String path, final String httpMethod);

    default Optional<Endpoint> findByPathAndMethod(final String path, final String method) {
        return findByPathAndHttpMethod(path, method);
    }
}
