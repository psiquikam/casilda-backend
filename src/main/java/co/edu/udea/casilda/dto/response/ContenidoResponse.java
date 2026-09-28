package co.edu.udea.casilda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO Response para la administración de contenidos destacados del Home
 * (panel del gestor de contenidos). Incluye campos internos (orden,
 * eliminado, auditoría) que no viajan en el endpoint público.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContenidoResponse {
    private Long id;
    private String imagen;
    private String titulo;
    private String contenido;
    private Instant vigenciaInicio;
    private Instant vigenciaFin;
    private String enlace;
    private String seccion;
    private Integer orden;
    private Boolean eliminado;
    private Instant createdAt;
    private String createdBy;
    private Instant modifiedAt;
    private String modifiedBy;
}
