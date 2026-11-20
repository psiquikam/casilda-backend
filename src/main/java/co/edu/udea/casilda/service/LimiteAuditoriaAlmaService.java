package co.edu.udea.casilda.service;

import co.edu.udea.casilda.exception.AtencionAlmaException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LimiteAuditoriaAlmaService {
    private final JdbcTemplate jdbc;

    @Value("${app.alma.auditoria.limite:30}")
    private int limite;

    @Value("${app.alma.auditoria.ventana-minutos:10}")
    private int ventanaMinutos;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consumir(Long usuarioId) {
        if (limite < 1 || ventanaMinutos < 1) {
            throw new IllegalStateException("Configuración inválida del límite de consulta de auditoría APH.");
        }
        int actualizados = jdbc.update("""
                INSERT INTO limiteauditoriaaph (idusuario, inicio, cantidad)
                VALUES (?, clock_timestamp(), 1)
                ON CONFLICT (idusuario) DO UPDATE
                SET inicio = CASE WHEN limiteauditoriaaph.inicio <= clock_timestamp() - (? * interval '1 minute')
                                  THEN clock_timestamp() ELSE limiteauditoriaaph.inicio END,
                    cantidad = CASE WHEN limiteauditoriaaph.inicio <= clock_timestamp() - (? * interval '1 minute')
                                    THEN 1 ELSE limiteauditoriaaph.cantidad + 1 END
                WHERE limiteauditoriaaph.inicio <= clock_timestamp() - (? * interval '1 minute')
                   OR limiteauditoriaaph.cantidad < ?
                """, usuarioId, ventanaMinutos, ventanaMinutos, ventanaMinutos, limite);
        if (actualizados == 0) {
            throw new AtencionAlmaException(HttpStatus.TOO_MANY_REQUESTS, "LIMITE_CONSULTAS",
                    "Se alcanzó el límite de consultas de auditoría. Intente más tarde.", Map.of());
        }
    }
}
