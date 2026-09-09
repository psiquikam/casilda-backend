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

class AtencionControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void atencionEndpoints() throws Exception {
        AtencionService service = mock(AtencionService.class);
        MockMvc m = mvc(new AtencionController(service));
        m.perform(post("/atenciones/pestana/5").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        for (int i : new int[]{6, 7, 8, 10, 11}) {
            m.perform(post("/atenciones/pestana/" + i).contentType("application/json").content(JSON))
                    .andExpect(status().isOk());
        }
        m.perform(post("/atenciones/pestana/9").contentType("application/json").content(JSON)).andExpect(status().isCreated());
    }
}
