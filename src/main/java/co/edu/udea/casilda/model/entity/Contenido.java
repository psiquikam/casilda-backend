package co.edu.udea.casilda.model.entity;

import co.edu.udea.casilda.model.enums.SeccionContenidoEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entidad para los contenidos destacados administrables por el gestor de contenidos,
 * que alimentan el Home público (secciones "¿Qué necesitas hacer hoy?" e
 * "Información de interés general").
 */
@Entity
@Table(name = "contenido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contenido extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ruta o URL de la imagen de la tarjeta. Solo se almacena la referencia;
     * el archivo se guarda fuera de la base de datos. Es opcional: una tarjeta
     * puede publicarse sin imagen (se pinta solo con título y contenido).
     */
    @Column(length = 500)
    private String imagen;

    @Column(nullable = false, length = 160)
    private String titulo;

    /**
     * Cuerpo del contenido. Puede ser tan extenso como un artículo completo,
     * por lo que no se limita a un varchar corto.
     */
    @Column(name = "contenido", nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "vigencia_inicio", nullable = false)
    private Instant vigenciaInicio;

    @Column(name = "vigencia_fin")
    private Instant vigenciaFin;

    @Column(length = 200)
    private String enlace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeccionContenidoEnum seccion;

    /**
     * Orden ascendente de despliegue en el Home; el título se usa como desempate.
     */
    @Column(nullable = false)
    private Integer orden = 0;

    /**
     * Borrado lógico: nunca se elimina físicamente, por trazabilidad.
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean eliminado = false;
}
