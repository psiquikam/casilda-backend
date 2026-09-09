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

class LineaAlmaControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void lineaAlmaEndpoints() throws Exception {
        LineaAlmaService service = mock(LineaAlmaService.class);
        MockMvc m = mvc(new LineaAlmaController(service));
        m.perform(get("/linea-alma/registros/1")).andExpect(status().isOk());
        m.perform(get("/linea-alma/registros")).andExpect(status().isOk());
        m.perform(post("/linea-alma/registros/1/contactos").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        m.perform(get("/linea-alma/registros/1/contactos")).andExpect(status().isOk());
        for (int i = 0; i <= 5; i++) {
            m.perform(post("/linea-alma/registros/pestana/" + i).contentType("application/json").content(JSON))
                    .andExpect(status().isOk());
        }
    }
}
