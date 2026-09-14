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

class AuthControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void authLogin() throws Exception {
        AuthService service = mock(AuthService.class);
        mvc(new AuthController(service)).perform(post("/auth/login").contentType("application/json").content(JSON))
                .andExpect(status().isOk());
    }
    @Test
    void authLoginRejectsInvalidRequest() throws Exception {
        AuthService service = mock(AuthService.class);
        MockMvc validatingMvc = MockMvcBuilders.standaloneSetup(new AuthController(service)).build();
        validatingMvc.perform(post("/auth/login").contentType("application/json").content(JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
