package co.edu.udea.casilda.service;

import co.edu.udea.casilda.dto.response.CasoListResponse;
import co.edu.udea.casilda.model.entity.Caso;
import co.edu.udea.casilda.repository.AtencionRepository;
import co.edu.udea.casilda.repository.CasoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasoServiceTest {

    @Mock
    private CasoRepository casoRepository;

    @Mock
    private AtencionRepository atencionRepository;

    @InjectMocks
    private CasoService casoService;

    @Test
    void listarCasosIncluyeCantidadDeAtencionesRegistradas() {
        Caso primero = new Caso();
        primero.setId(14L);
        Caso segundo = new Caso();
        segundo.setId(16L);

        when(casoRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(primero, segundo)));
        when(atencionRepository.countByCasoId(14L)).thenReturn(1L);
        when(atencionRepository.countByCasoId(16L)).thenReturn(3L);

        Page<CasoListResponse> resultado = casoService.listarCasosPaginados(0, 10);

        assertEquals(1L, resultado.getContent().get(0).getCantidadAtenciones());
        assertEquals(3L, resultado.getContent().get(1).getCantidadAtenciones());
    }
}
