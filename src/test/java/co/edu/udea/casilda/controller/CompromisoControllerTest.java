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

class CompromisoControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void compromisoEndpoints() throws Exception {
        CompromisoService service = mock(CompromisoService.class);
        MockMvc m = mvc(new CompromisoController(service));
        m.perform(get("/compromisos/persona/1")).andExpect(status().isOk());
        m.perform(post("/compromisos/persona").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        m.perform(delete("/compromisos/persona/1/2")).andExpect(status().isNoContent());
        m.perform(get("/compromisos/profesional/1")).andExpect(status().isOk());
        m.perform(post("/compromisos/profesional").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        m.perform(delete("/compromisos/profesional/1/2")).andExpect(status().isNoContent());
    }
}
