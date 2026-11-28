package co.edu.udea.casilda.service;

import co.edu.udea.casilda.model.entity.RutaActivacion;
import co.edu.udea.casilda.model.entity.RutaEquipoAtencion;
import co.edu.udea.casilda.repository.RutaEquipoAtencionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaestroEquipoAtencionTest {
    @Mock
    private RutaEquipoAtencionRepository rutas;
    @InjectMocks
    private MaestroService service;

    @Test
    void reconoceLaConsultaDeRutasActivasPorTipo() {
        assertDoesNotThrow(() ->
                new PartTree("findByTipoAndRutaActivoTrueOrderByRutaIdAsc", RutaEquipoAtencion.class));
    }

    @Test
    void retornaElIdDeLaRutaCompartidaNoElDeSuClasificacion() {
        RutaActivacion ruta = new RutaActivacion();
        ruta.setId(5);
        ruta.setCodigo("SEGURIDAD");
        ruta.setNombre("Seguridad");
        RutaEquipoAtencion clasificacion = new RutaEquipoAtencion();
        clasificacion.setId(99);
        clasificacion.setRuta(ruta);
        when(rutas.findByTipoAndRutaActivoTrueOrderByRutaIdAsc("INTERNA")).thenReturn(List.of(clasificacion));

        var resultado = service.obtenerRutasEquipoAtencion("INTERNA");

        assertEquals(5L, resultado.get(0).getId());
        assertEquals("SEGURIDAD", resultado.get(0).getCodigo());
    }

}
