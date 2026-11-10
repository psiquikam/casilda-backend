package co.edu.udea.casilda.model.enums;

import lombok.Getter;

/**
 * Enumeración para tipos de servicio.
 */
@Getter
public enum TipoServicioEnum {
    JURIDICO(1, "Jurídico"),
    DUPLA_PSICOJURIDICA(2, "Dupla Psicojurídica"),
    PSICOLOGIA(3, "Psicología"),
    PSICOORIENTACION(4, "Psicoorientación"),
    TRABAJO_SOCIAL(5, "Trabajo Social");

    private final Integer id;
    private final String nombre;

    TipoServicioEnum(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /**
     * Obtiene el tipo de servicio por su ID.
     *
     * @param id ID del tipo de servicio
     * @return TipoServicioEnum correspondiente
     * @throws IllegalArgumentException si el ID no es válido
     */
    public static TipoServicioEnum fromId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del tipo de servicio no puede ser nulo");
        }
        for (TipoServicioEnum tipo : values()) {
            if (tipo.id.equals(id)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de servicio inválido: " + id);
    }
}