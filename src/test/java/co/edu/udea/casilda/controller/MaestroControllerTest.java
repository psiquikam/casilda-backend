package co.edu.udea.casilda.controller;

import co.edu.udea.casilda.dto.response.MaestroDTO;
import co.edu.udea.casilda.service.MaestroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaestroControllerTest {

    private static final String JSON = "{}";

    private MockMvc mockMvc;

    private MaestroService catalogoService;

    @BeforeEach
    void setUp() {
        catalogoService = mock(MaestroService.class);
        MaestroController maestroController = new MaestroController(catalogoService);
        mockMvc = MockMvcBuilders.standaloneSetup(maestroController).build();
    }

    @Test
    void debeRetornarLugaresEntrevistaParaCampoLugarEntrevista() throws Exception {
        List<MaestroDTO> lugaresEntrevista = List.of(
                MaestroDTO.builder().id(1L).codigo(null).nombre("Presencial").build(),
                MaestroDTO.builder().id(2L).codigo(null).nombre("Virtual").build(),
                MaestroDTO.builder().id(3L).codigo(null).nombre("Telefonica").build()
        );

        when(catalogoService.obtenerLugaresEntrevista()).thenReturn(lugaresEntrevista);

        mockMvc.perform(get("/maestros/lugares-entrevista"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Presencial"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].nombre").value("Virtual"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].nombre").value("Telefonica"));
    }

    private MockMvc mvc(Object controller) {
        Validator noValidation = new Validator() {
            public boolean supports(Class<?> type) { return true; }
            public void validate(Object target, Errors errors) { }
        };
        return MockMvcBuilders.standaloneSetup(controller).setValidator(noValidation).build();
    }

    @Test
    void maestroReadEndpoints() throws Exception {
        MaestroService service = mock(MaestroService.class);
        MockMvc m = mvc(new MaestroController(service));
        String[] paths = {
                "/maestros/paises", "/maestros/sexos", "/maestros/tipos-identificacion",
                "/maestros/etnias", "/maestros/identidades-genero", "/maestros/orientaciones-sexuales",
                "/maestros/tipo-discapacidad", "/maestros/subtipo-discapacidad/1", "/maestros/departamentos",
                "/maestros/municipios", "/maestros/ciudades", "/maestros/departamentos/1/ciudades",
                "/maestros/departamentos/codigo/05/ciudades", "/maestros/campus",
                "/maestros/unidades-administrativas", "/maestros/unidades-academicas", "/maestros/roles",
                "/maestros/vinculos-agresor-victima", "/maestros/vinculos-udea", "/maestros/formas-ocurrencia",
                "/maestros/lugares-ocurrencia", "/maestros/actividades-misionales", "/maestros/tipos-violencia",
                "/maestros/modalidades-violencia", "/maestros/modalidades-violencia/tipo/1",
                "/maestros/modalidades-violencia-sexual", "/maestros/cargos", "/maestros/tipos-solicitud",
                "/maestros/medio-solicitud", "/maestros/tiempos-ocurrido-unidad",
                "/maestros/catalogos/tipos-solicitud/paginado?page=0&size=10", "/maestros/programas",
                "/maestros/programas?unidadAcademicaId=1&pregrado=true", "/maestros/resultados-contacto-telefonico",
                "/maestros/regimenes", "/maestros/eps", "/maestros/grupos-atencion",
                "/maestros/estados-atencion", "/maestros/estados-caso", "/maestros/tipos-correo",
                "/maestros/tipos-telefono", "/maestros/tipos-reporte-alma", "/maestros/canales-contacto",
                "/maestros/lugares-entrevista", "/maestros/protocolos-aph", "/maestros/resultados-triage",
                "/maestros/tipos-asignacion", "/maestros/tipos-servicio", "/maestros/motivos-estado-cita",
                "/maestros/apreciaciones", "/maestros/tipos-apreciacion/1", "/maestros/tipos-ruta-activacion",
                "/maestros/rutas-activacion", "/maestros/tipos-remision", "/maestros/instancias-remision?tipoRemisionId=1",
                "/maestros/tipos-compromiso", "/maestros/motivos-estado-seguimiento", "/maestros/tipos-seguimiento",
                "/maestros/acciones", "/maestros/actividades/por-accion/1", "/maestros/estados-seguimiento",
                "/maestros/seguimientos-atencion", "/maestros/tipos-medida", "/maestros/subtipos-medida",
                "/maestros/subtipos-medida/1", "/maestros/responsables-medida", "/maestros/actores-remitentes"
        };
        for (String path : paths) {
            m.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void maestroWriteEndpoints() throws Exception {
        MaestroService service = mock(MaestroService.class);
        MockMvc m = mvc(new MaestroController(service));
        String[] resources = {"tipos-solicitud", "campus", "unidades-administrativas", "unidades-academicas", "tipos-identificacion"};
        for (String resource : resources) {
            m.perform(post("/maestros/" + resource).contentType("application/json").content(JSON))
                    .andExpect(status().isCreated());
            m.perform(put("/maestros/" + resource + "/1").contentType("application/json").content(JSON))
                    .andExpect(status().isOk());
            m.perform(delete("/maestros/" + resource + "/1")).andExpect(status().isNoContent());
        }
    }
}
