package co.edu.udea.casilda.model.enums;

import lombok.Getter;

/**
 * Enumeración para los estados de la cita de atención
 */
@Getter
public enum EstadoCitaEnum {
    PROGRAMADA(1, "Programada"),
    CANCELADA(2, "Cancelada"),
    REPROGRAMADA(3, "Reprogramada"),
    ATENDIDA(4, "Atendida"),
    INASISTENCIA(5, "Inasistencia");

    private final Integer id;
    private final String nombre;

    EstadoCitaEnum(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /**
     * Obtiene el estado de cita por su ID
     * @param id ID del estado
     * @return EstadoCitaEnum correspondiente
     * @throws IllegalArgumentException si el ID no es válido
     */
    public static EstadoCitaEnum fromId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del estado de cita no puede ser nulo");
        }
        for (EstadoCitaEnum estado : values()) {
            if (estado.id.equals(id)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de cita inválido: " + id);
    }
}
