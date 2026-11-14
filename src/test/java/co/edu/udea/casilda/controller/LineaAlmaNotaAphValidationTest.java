package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.exception.GlobalExceptionHandler;
import co.edu.udea.casilda.service.LineaAlmaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LineaAlmaNotaAphValidationTest {
    private LineaAlmaService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(LineaAlmaService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new LineaAlmaController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void aceptaNotasHastaElLimite() throws Exception {
        for (int longitud : new int[]{1000, 2000}) {
            mvc.perform(post("/linea-alma/registros/pestana/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request("a".repeat(longitud))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void rechazaNotaQueSuperaElLimiteConErrorDeCampo() throws Exception {
        mvc.perform(post("/linea-alma/registros/pestana/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors['atencionAph.notaAph']")
                        .value("La nota APH no puede exceder 2000 caracteres"));

        verify(service, never()).registrarPestana3(any());
    }

    private String request(String nota) {
        return """
                {"id":4,"atencionAph":{"fechaHora":"2026-11-09T11:30:00",
                "idProtocoloAph":1,"practicoTriage":true,"idResultadoTriage":1,
                "notaAph":"%s","aceptaPsicologia":true,"requiereRemision":true}}
                """.formatted(nota);
    }
}
