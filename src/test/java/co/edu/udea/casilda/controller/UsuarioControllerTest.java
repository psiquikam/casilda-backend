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

class UsuarioControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void usuarioEndpoints() throws Exception {
        UsuarioService service = mock(UsuarioService.class);
        when(service.obtenerTodos()).thenReturn(List.of());
        when(service.obtenerPaginados(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        MockMvc m = mvc(new UsuarioController(service));
        m.perform(get("/usuarios")).andExpect(status().isOk());
        m.perform(get("/usuarios/paginado")).andExpect(status().isOk());
        m.perform(get("/usuarios/1")).andExpect(status().isOk());
        m.perform(post("/usuarios").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        m.perform(put("/usuarios/1").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(delete("/usuarios/1")).andExpect(status().isOk());
        m.perform(patch("/usuarios/1/estado").contentType("application/json").content("{\"activo\":true}"))
                .andExpect(status().isOk());
    }
}
