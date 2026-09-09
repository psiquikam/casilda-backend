package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.repository.ParametroSistemaRepository;
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
        MockMvc m = mvc(new CitaController(service));
        m.perform(get("/citas")).andExpect(status().isOk());
        m.perform(get("/citas/paginado").param("page", "1").param("size", "5").param("idEstadoCita", "2")
                .param("excluirEstadoCitaId", "3")).andExpect(status().isOk());
        m.perform(put("/citas/7/reprogramar").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(put("/citas/7/cancelar").contentType("application/json").content(JSON)).andExpect(status().isOk());
    }
}
