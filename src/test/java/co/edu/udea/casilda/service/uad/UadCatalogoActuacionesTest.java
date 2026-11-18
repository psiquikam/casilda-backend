package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.response.uad.UadResponses;
import co.edu.udea.casilda.model.enums.uad.CodigoActuacion;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.EstadoGestionUad;
import co.edu.udea.casilda.model.enums.uad.FaseUad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.udea.casilda.model.enums.uad.CodigoActuacion.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mismo catálogo que {@code dominio/catalogo-actuaciones.ts} del frontend (bitácora §7 y §8). */
class UadCatalogoActuacionesTest {

    private static List<CodigoActuacion> ordinarias(final FaseUad fase, final EquipoUad equipo) {
        return UadCatalogoActuaciones.paraCaso(fase, equipo, EstadoGestionUad.ACTIVA).stream()
                .filter(a -> !a.permanente()).map(UadResponses.ActuacionDisponible::codigo).toList();
    }

    private static UadResponses.ActuacionDisponible buscar(final List<UadResponses.ActuacionDisponible> lista, final CodigoActuacion c) {
        return lista.stream().filter(a -> a.codigo() == c).findFirst().orElseThrow();
    }

    @Test
    void ordinariasPorFaseYEquipo() {
        assertEquals(List.of(INDAGACION_PREVIA, INVESTIGACION_DISCIPLINARIA, PRACTICA_PRUEBAS, PLIEGO_CARGOS),
                ordinarias(FaseUad.INSTRUCCION, EquipoUad.EQUIPO_3));
        assertEquals(List.of(AVERIGUACIONES_PRELIMINARES, PRACTICA_PRUEBAS, PLIEGO_CARGOS), ordinarias(FaseUad.INSTRUCCION, EquipoUad.EQUIPO_4));
        assertEquals(List.of(PRACTICA_PRUEBAS, AUDIENCIAS, MEDIACION, FALLO, REPOSICION_APELACION), ordinarias(FaseUad.DECISION, EquipoUad.EQUIPO_3));
        assertEquals(ordinarias(FaseUad.DECISION, EquipoUad.EQUIPO_3), ordinarias(FaseUad.DECISION, EquipoUad.EQUIPO_4));
    }

    @Test
    void permanentesSiempreVisiblesYLasPendientesDeshabilitadas() {
        final List<UadResponses.ActuacionDisponible> lista = UadCatalogoActuaciones.paraCaso(FaseUad.INSTRUCCION, EquipoUad.EQUIPO_3, EstadoGestionUad.ACTIVA);
        assertEquals(List.of(REMISION_COMPETENCIA, INHIBITORIO, TERMINACION, FINALIZAR_INSTRUCCION, REMISION_RUTA),
                lista.stream().filter(UadResponses.ActuacionDisponible::permanente).map(UadResponses.ActuacionDisponible::codigo).toList());
        assertTrue(buscar(lista, REMISION_COMPETENCIA).ejecutable());
        assertTrue(buscar(lista, FINALIZAR_INSTRUCCION).ejecutable());
        assertFalse(buscar(lista, INHIBITORIO).ejecutable());
        assertTrue(buscar(lista, INHIBITORIO).pendienteValidacion());
    }

    @Test
    void enDecisionNoSeFinalizaInstruccionYMediacionEstaPendiente() {
        final List<UadResponses.ActuacionDisponible> lista = UadCatalogoActuaciones.paraCaso(FaseUad.DECISION, EquipoUad.EQUIPO_4, EstadoGestionUad.ACTIVA);
        assertFalse(buscar(lista, FINALIZAR_INSTRUCCION).ejecutable());
        assertFalse(buscar(lista, MEDIACION).ejecutable());
        assertTrue(buscar(lista, MEDIACION).pendienteValidacion());
    }

    @Test
    void gestionFinalizadaDeshabilitaTodo() {
        assertTrue(UadCatalogoActuaciones.paraCaso(FaseUad.DECISION, EquipoUad.EQUIPO_3, EstadoGestionUad.FINALIZADA).stream()
                .noneMatch(UadResponses.ActuacionDisponible::ejecutable));
    }

    @Test
    void validarRechazaRepetidasPermanentesYNoDisponibles() {
        final List<UadResponses.ActuacionDisponible> catalogo = UadCatalogoActuaciones.paraCaso(FaseUad.INSTRUCCION, EquipoUad.EQUIPO_4, EstadoGestionUad.ACTIVA);
        assertTrue(UadCatalogoActuaciones.validar(List.of(PRACTICA_PRUEBAS, PLIEGO_CARGOS), catalogo).isEmpty());
        assertTrue(UadCatalogoActuaciones.validar(List.of(PLIEGO_CARGOS, PLIEGO_CARGOS), catalogo).containsKey("actuaciones"));
        assertTrue(UadCatalogoActuaciones.validar(List.of(REMISION_COMPETENCIA), catalogo).containsKey("actuaciones"));
        assertTrue(UadCatalogoActuaciones.validar(List.of(INDAGACION_PREVIA), catalogo).containsKey("actuaciones"));
        assertTrue(UadCatalogoActuaciones.validar(List.of(), catalogo).containsKey("actuaciones"));
    }
}
