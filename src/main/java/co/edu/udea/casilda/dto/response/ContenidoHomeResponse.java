package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO Response público para un contenido destacado del Home.
 * La forma de este DTO está fijada por el contrato de "Contenido dinámico
 * del Home" acordado con el equipo de frontend: no debe renombrarse ni
 * envolverse en un objeto adicional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContenidoHomeResponse {
    private Long id;
    private String imagen;
    private String titulo;
    private String contenido;
    private Instant vigenciaInicio;
    private Instant vigenciaFin;
    private String enlace;
    private String seccion;
}
