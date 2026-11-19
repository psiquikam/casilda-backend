package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.repository.ParametroSistemaRepository;
import co.edu.udea.casilda.dto.request.ProgramarCitaSeguimientoRequest;
import co.edu.udea.casilda.dto.request.RegistrarResultadoCitaRequest;
import co.edu.udea.casilda.dto.response.CitaResponse;
import co.edu.udea.casilda.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;

class CitaControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void citaEndpoints() throws Exception {
        CitaService service = mock(CitaService.class);
        when(service.listarTodasLasCitas()).thenReturn(List.of());
        when(service.listarCitasPaginadas(anyInt(), anyInt(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(service.listarCitasPendientesDeCaso(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(service.listarHistorialAtenciones(5L)).thenReturn(List.of());
        when(service.programarSeguimiento(eq(5L), any(ProgramarCitaSeguimientoRequest.class)))
                .thenReturn(new CitaResponse());
        when(service.registrarResultado(eq(7L), any(RegistrarResultadoCitaRequest.class)))
                .thenReturn(new CitaResponse());
        MockMvc m = mvc(new CitaController(service));
        m.perform(get("/citas")).andExpect(status().isOk());
        m.perform(get("/citas/paginado").param("page", "1").param("size", "5").param("idEstadoCita", "2")
                .param("excluirEstadoCitaId", "3")).andExpect(status().isOk());
        m.perform(get("/citas/pendientes-caso/paginado")).andExpect(status().isOk());
        m.perform(get("/citas/caso/5/historial")).andExpect(status().isOk());
        m.perform(post("/citas/caso/5/seguimientos").contentType("application/json")
                .content("{\"fechaCita\":\"2099-01-02\",\"horaCita\":\"10:30\"}"))
                .andExpect(status().isCreated());
        m.perform(put("/citas/7/resultado").contentType("application/json")
                .content("{\"resultado\":\"ATENDIDA\"}")).andExpect(status().isOk());
        m.perform(put("/citas/7/reprogramar").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(put("/citas/7/cancelar").contentType("application/json").content(JSON)).andExpect(status().isOk());
    }
}
