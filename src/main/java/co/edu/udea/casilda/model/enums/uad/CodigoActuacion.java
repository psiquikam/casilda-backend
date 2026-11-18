package co.edu.udea.casilda.model.enums.uad;

/**
 * Actuaciones de «Gestionar caso»: ordinarias (bitácora §7) y acciones permanentes (§8). Las reglas de
 * fase, equipo y disponibilidad están en {@code UadCatalogoActuaciones}.
 *
 * <p>Contrato: {@code CodigoActuacion} en {@code docs/contratos/uad/openapi-uad.yaml} del frontend.</p>
 */
public enum CodigoActuacion {
    INDAGACION_PREVIA("Indagación previa"),
    INVESTIGACION_DISCIPLINARIA("Investigación disciplinaria"),
    AVERIGUACIONES_PRELIMINARES("Averiguaciones preliminares"),
    PRACTICA_PRUEBAS("Práctica de pruebas"),
    PLIEGO_CARGOS("Pliego de cargos"),
    AUDIENCIAS("Audiencias"),
    MEDIACION("Mediación"),
    FALLO("Fallo"),
    REPOSICION_APELACION("Reposición y/o apelación"),
    REMISION_COMPETENCIA("Remisión por competencia"),
    INHIBITORIO("Inhibitorio"),
    TERMINACION("Terminación"),
    FINALIZAR_INSTRUCCION("Finalizar etapa de instrucción"),
    REMISION_RUTA("Remisión a otro actor de la Ruta");

    private final String nombre;

    CodigoActuacion(final String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
