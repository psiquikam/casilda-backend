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

class SolicitudControllerTest {
    private static final String JSON = "{}";

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void solicitudEndpoints() throws Exception {
        SolicitudAcompanamientoService service = mock(SolicitudAcompanamientoService.class);
        when(service.listarPaginadas(anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        MockMvc m = mvc(new SolicitudController(service));
        m.perform(post("/solicitudes/acompanamiento").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        m.perform(get("/solicitudes/acompanamiento/1")).andExpect(status().isOk());
        m.perform(get("/solicitudes/acompanamiento").param("idEstadoSolicitud", "2")).andExpect(status().isOk());
        m.perform(get("/solicitudes/acompanamiento/paginado").param("page", "1").param("size", "4"))
                .andExpect(status().isOk());
        m.perform(delete("/solicitudes/acompanamiento/1")).andExpect(status().isNoContent());
        m.perform(put("/solicitudes/acompanamiento/1").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(post("/solicitudes/acompanamiento/1/asignar").contentType("application/json").content(JSON)).andExpect(status().isOk());
        m.perform(get("/solicitudes/grupos-profesionales")).andExpect(status().isOk());
        m.perform(post("/solicitudes/acompanamiento/1/contacto").contentType("application/json").content(JSON)).andExpect(status().isCreated());
        m.perform(get("/solicitudes/acompanamiento/1/contactos")).andExpect(status().isOk());
    }
}
