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

class CasoControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void casoEndpoints() throws Exception {
        CasoService service = mock(CasoService.class);
        MockMvc m = mvc(new CasoController(service));
        m.perform(get("/casos/paginado")).andExpect(status().isOk());
        m.perform(post("/casos/pestana/0").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        m.perform(post("/casos/pestana/1").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(post("/casos/pestana/2").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(post("/casos/pestana/3").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(post("/casos/pestana/4").contentType("application/json").content(JSON)).andExpect(status().isOk());
    }
}
