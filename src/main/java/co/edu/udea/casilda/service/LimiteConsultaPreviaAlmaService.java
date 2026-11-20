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
public class LimiteConsultaPreviaAlmaService {
    private final JdbcTemplate jdbc;

    @Value("${app.alma.consulta-previa.limite:30}")
    private int limite;

    @Value("${app.alma.consulta-previa.ventana-minutos:10}")
    private int ventanaMinutos;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consumir(Long usuarioId) {
        if (limite < 1 || ventanaMinutos < 1) {
            throw new IllegalStateException("Configuración inválida del límite de consulta previa APH.");
        }
        int actualizados = jdbc.update("""
                INSERT INTO limiteconsultapreviaaph (idusuario, inicio, cantidad)
                VALUES (?, clock_timestamp(), 1)
                ON CONFLICT (idusuario) DO UPDATE
                SET inicio = CASE WHEN limiteconsultapreviaaph.inicio <= clock_timestamp() - (? * interval '1 minute')
                                  THEN clock_timestamp() ELSE limiteconsultapreviaaph.inicio END,
                    cantidad = CASE WHEN limiteconsultapreviaaph.inicio <= clock_timestamp() - (? * interval '1 minute')
                                    THEN 1 ELSE limiteconsultapreviaaph.cantidad + 1 END
                WHERE limiteconsultapreviaaph.inicio <= clock_timestamp() - (? * interval '1 minute')
                   OR limiteconsultapreviaaph.cantidad < ?
                """, usuarioId, ventanaMinutos, ventanaMinutos, ventanaMinutos, limite);
        if (actualizados == 0) {
            throw new AtencionAlmaException(HttpStatus.TOO_MANY_REQUESTS, "LIMITE_CONSULTAS",
                    "Se alcanzó el límite de consultas. Intente más tarde.", Map.of());
        }
    }
}
