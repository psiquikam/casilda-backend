package co.edu.udea.casilda.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaRecursosService {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarConsultaPreviaPersona(String email, Long idPersona) {
        Long idUsuario = jdbc.queryForObject("SELECT id FROM usuario WHERE lower(email) = lower(?)", Long.class, email);
        jdbc.update("""
                INSERT INTO auditoriarecursos
                    (idusuario, usuario, accion, recurso, idrecurso, tablafectada,
                     idregistro, campos, endpoint, idoperacion)
                VALUES (?, ?, ?, 'PERSONA', ?, 'persona', ?, '{}', '/alma/personas/consulta', ?)
                """, idUsuario, email, idPersona == null ? "CONSULTA_PREVIA_SIN_COINCIDENCIA" : "CONSULTA_PREVIA",
                idPersona == null ? null : idPersona.toString(),
                idPersona == null ? null : idPersona.toString(), UUID.randomUUID());
    }
}
