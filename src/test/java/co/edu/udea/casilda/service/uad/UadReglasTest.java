package co.edu.udea.casilda.service.uad;

import co.edu.udea.casilda.dto.request.uad.DecisionEvaluacionRequest;
import co.edu.udea.casilda.model.enums.uad.EquipoUad;
import co.edu.udea.casilda.model.enums.uad.ModoCaso;
import co.edu.udea.casilda.model.enums.uad.TipoDecision;
import co.edu.udea.casilda.model.enums.uad.TipoNoticia;
import co.edu.udea.casilda.model.enums.uad.VinculacionInvestigado;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas puras UAD: competencia, mapeo provisional de vinculación y validación de decisiones. */
class UadReglasTest {

    @Nested
    class Competencia {

        @Test
        void estudiantesInstruyeEquipo4YDecideEquipo3() {
            assertEquals(EquipoUad.EQUIPO_4, UadCompetencia.equipoInstructor(VinculacionInvestigado.ESTUDIANTE_POSGRADO));
            assertEquals(EquipoUad.EQUIPO_3, UadCompetencia.equipoDecisor(VinculacionInvestigado.ESTUDIANTE_PREGRADO));
        }

        @Test
        void personalInstruyeEquipo3YDecideEquipo4() {
            for (VinculacionInvestigado v : UadCompetencia.PERSONAL) {
                assertEquals(EquipoUad.EQUIPO_3, UadCompetencia.equipoInstructor(v), v.name());
                assertEquals(EquipoUad.EQUIPO_4, UadCompetencia.equipoDecisor(v), v.name());
            }
        }

        @Test
        void ndaYNoSeSonVisiblesAAmbos() {
            assertEquals(List.of(EquipoUad.EQUIPO_3, EquipoUad.EQUIPO_4),
                    UadCompetencia.equiposConVisibilidad(TipoNoticia.NDA, VinculacionInvestigado.ESTUDIANTE_PREGRADO));
            assertEquals(List.of(EquipoUad.EQUIPO_3, EquipoUad.EQUIPO_4),
                    UadCompetencia.equiposConVisibilidad(TipoNoticia.NDI, VinculacionInvestigado.NO_SE));
            assertFalse(UadCompetencia.esVisiblePara(List.of(EquipoUad.EQUIPO_4), TipoNoticia.NDI, VinculacionInvestigado.DIRECTIVO));
        }

        @Test
        void vinculacionesVisiblesIncluyenIndeterminadas() {
            assertTrue(UadCompetencia.vinculacionesVisibles(List.of(EquipoUad.EQUIPO_4)).contains(VinculacionInvestigado.NO_TIENE_VINCULO));
            assertFalse(UadCompetencia.vinculacionesVisibles(List.of(EquipoUad.EQUIPO_4)).contains(VinculacionInvestigado.DOCENTE_VINCULADO));
        }
    }

    @Nested
    class MapeoVinculacion {

        @Test
        void homologaElCatalogoVinculoUdeA() {
            assertEquals(VinculacionInvestigado.ESTUDIANTE_PREGRADO, UadMapeoVinculacion.desdeVinculoUdeA(1));
            assertEquals(VinculacionInvestigado.ADMINISTRATIVO, UadMapeoVinculacion.desdeVinculoUdeA(5));
            assertEquals(VinculacionInvestigado.DOCENTE_CATEDRA, UadMapeoVinculacion.desdeVinculoUdeA(11));
            assertEquals(VinculacionInvestigado.NO_TIENE_VINCULO, UadMapeoVinculacion.desdeVinculoUdeA(14));
            assertEquals(VinculacionInvestigado.NO_SE, UadMapeoVinculacion.desdeVinculoUdeA(10));
        }

        @Test
        void sinDatoODesconocidoEsNoSe() {
            assertEquals(VinculacionInvestigado.NO_SE, UadMapeoVinculacion.desdeVinculoUdeA(null));
            assertEquals(VinculacionInvestigado.NO_SE, UadMapeoVinculacion.desdeVinculoUdeA(999));
        }
    }

    @Nested
    class ValidacionDecision {

        private final LocalDate hoy = LocalDate.of(2026, 11, 14);

        private UadDecisionValidator.Contexto ctx(final String casoIdActual, final List<String> asociados) {
            return new UadDecisionValidator.Contexto(TipoNoticia.NDI, casoIdActual, asociados, true, true, hoy, c -> true, c -> true, c -> true);
        }

        private DecisionEvaluacionRequest continuar(final ModoCaso modo, final String vincular, final Boolean confirma) {
            return new DecisionEvaluacionRequest(TipoDecision.CONTINUAR_TRAMITE, 1L, null,
                    new DecisionEvaluacionRequest.ContinuarTramite(modo, vincular, confirma, null), null, null, null);
        }

        @Test
        void rechazaDecisionesIncompatibles() {
            final DecisionEvaluacionRequest c = new DecisionEvaluacionRequest(TipoDecision.INHIBITORIO, 1L, null, null, null,
                    new DecisionEvaluacionRequest.Inhibitorio("X", "y"),
                    new DecisionEvaluacionRequest.RemisionCompetencia("EVALUACION_PRELIMINAR", "E", hoy, null));
            assertNotNull(UadDecisionValidator.validar(c, ctx(null, List.of())).get("tipo"));
        }

        @Test
        void expedienteUadEsOpcional() {
            assertTrue(UadDecisionValidator.validar(continuar(ModoCaso.CREAR_NUEVO, null, null), ctx(null, List.of())).isEmpty());
        }

        @Test
        void vincularExigeCasoConsultadoYConfirmacion() {
            final Map<String, String> sinConsulta = UadDecisionValidator.validar(continuar(ModoCaso.VINCULAR_EXISTENTE, "CAS-1", true), ctx(null, List.of()));
            assertNotNull(sinConsulta.get("continuarTramite.casoIdVincular"));
            final Map<String, String> sinConfirmar = UadDecisionValidator.validar(continuar(ModoCaso.VINCULAR_EXISTENTE, "CAS-1", false), ctx(null, List.of("CAS-1")));
            assertNotNull(sinConfirmar.get("continuarTramite.confirmaMismosHechos"));
            assertTrue(UadDecisionValidator.validar(continuar(ModoCaso.VINCULAR_EXISTENTE, "CAS-1", true), ctx(null, List.of("CAS-1"))).isEmpty());
        }

        @Test
        void conCasoAsignadoSoloSeConserva() {
            assertNotNull(UadDecisionValidator.validar(continuar(ModoCaso.CREAR_NUEVO, null, null), ctx("CAS-9", List.of())).get("continuarTramite.modoCaso"));
            assertTrue(UadDecisionValidator.validar(continuar(ModoCaso.CONSERVAR_ASIGNADO, null, null), ctx("CAS-9", List.of())).isEmpty());
        }

        @Test
        void ndaNoAdmiteSolicitudDeInformacion() {
            final DecisionEvaluacionRequest c = new DecisionEvaluacionRequest(TipoDecision.SOLICITAR_INFORMACION, 1L, null, null,
                    new DecisionEvaluacionRequest.SolicitudInformacion("A", "Precise las fechas de los hechos.", hoy.plusDays(5)), null, null);
            final UadDecisionValidator.Contexto nda = new UadDecisionValidator.Contexto(TipoNoticia.NDA, null, List.of(), false, true, hoy, x -> true, x -> true, x -> true);
            assertNotNull(UadDecisionValidator.validar(c, nda).get("tipo"));
        }

        @Test
        void remisionSinRadicadoYSinFechaFutura() {
            final DecisionEvaluacionRequest valida = new DecisionEvaluacionRequest(TipoDecision.REMISION_COMPETENCIA, 1L, null, null, null, null,
                    new DecisionEvaluacionRequest.RemisionCompetencia("EVALUACION_PRELIMINAR", "E", hoy, null));
            assertTrue(UadDecisionValidator.validar(valida, ctx(null, List.of())).isEmpty());
            final DecisionEvaluacionRequest futura = new DecisionEvaluacionRequest(TipoDecision.REMISION_COMPETENCIA, 1L, null, null, null, null,
                    new DecisionEvaluacionRequest.RemisionCompetencia("EVALUACION_PRELIMINAR", "E", hoy.plusDays(1), null));
            assertNotNull(UadDecisionValidator.validar(futura, ctx(null, List.of())).get("remisionCompetencia.fechaRemision"));
        }

        @Test
        void continuarExigeVinculacionDeterminada() {
            final UadDecisionValidator.Contexto sinVinculo = new UadDecisionValidator.Contexto(TipoNoticia.NDI, null, List.of(), true, false, hoy, x -> true, x -> true, x -> true);
            assertNotNull(UadDecisionValidator.validar(continuar(ModoCaso.CREAR_NUEVO, null, null), sinVinculo).get("continuarTramite.vinculacion"));
        }
    }
}
