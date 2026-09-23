package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.request.ContenidoRequest;
import co.edu.udea.casilda.dto.response.ContenidoHomeResponse;
import co.edu.udea.casilda.dto.response.ContenidoResponse;
import co.edu.udea.casilda.service.ContenidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContenidoControllerTest {

    private static final String CONTENIDO_VALIDO = """
            {
              "imagen": "assets/linea_alma.svg",
              "titulo": "Atención por Línea Alma",
              "contenido": "Línea de escucha y apoyo psicológico inmediato.",
              "vigenciaInicio": "2026-01-01T00:00:00Z",
              "vigenciaFin": null,
              "enlace": null,
              "seccion": "acciones"
            }
            """;

    private MockMvc mockMvc;
    private ContenidoService service;

    @BeforeEach
    void setUp() {
        service = mock(ContenidoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ContenidoController(service)).build();
    }

    @Test
    void obtenerContenidosHomeDevuelveArregloPlanoConCacheControl() throws Exception {
        ContenidoHomeResponse item = ContenidoHomeResponse.builder()
                .id(1L)
                .imagen("assets/linea_alma.svg")
                .titulo("Atención por Línea Alma")
                .contenido("Línea de escucha y apoyo psicológico inmediato.")
                .vigenciaInicio(Instant.parse("2026-01-01T00:00:00Z"))
                .vigenciaFin(null)
                .enlace(null)
                .seccion("acciones")
                .build();
        when(service.listarVigentesHome()).thenReturn(List.of(item));

        mockMvc.perform(get("/contenidos/home"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].seccion").value("acciones"))
                .andExpect(jsonPath("$[0].vigenciaFin").doesNotExist());
    }

    @Test
    void obtenerContenidosHomeRetornaListaVaciaComo200() throws Exception {
        when(service.listarVigentesHome()).thenReturn(List.of());

        mockMvc.perform(get("/contenidos/home"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listarPaginadoDevuelvePagina() throws Exception {
        ContenidoResponse item = ContenidoResponse.builder().id(1L).titulo("Título").build();
        when(service.listarPaginado(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/contenidos").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void obtenerPorIdDevuelveContenido() throws Exception {
        when(service.obtenerPorId(1L)).thenReturn(ContenidoResponse.builder().id(1L).titulo("Título").build());

        mockMvc.perform(get("/contenidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crearContenidoValidoDevuelve201() throws Exception {
        when(service.crear(any(ContenidoRequest.class)))
                .thenReturn(ContenidoResponse.builder().id(9L).titulo("Atención por Línea Alma").build());

        mockMvc.perform(post("/contenidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONTENIDO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void crearContenidoInvalidoDevuelve400ConErroresPorCampo() throws Exception {
        String invalido = """
                {
                  "titulo": "",
                  "contenido": "",
                  "seccion": "otra"
                }
                """;

        mockMvc.perform(post("/contenidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarContenidoDevuelve200() throws Exception {
        when(service.actualizar(eq(1L), any(ContenidoRequest.class)))
                .thenReturn(ContenidoResponse.builder().id(1L).titulo("Actualizado").build());

        mockMvc.perform(put("/contenidos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONTENIDO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Actualizado"));
    }

    @Test
    void eliminarContenidoDevuelve204() throws Exception {
        mockMvc.perform(delete("/contenidos/1"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }
}
