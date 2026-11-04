package br.com.sistemas.chamados.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;

@WebMvcTest(TecnicoController.class)
class TecnicoControllerTest {

    private static final String JSON_VALIDO =
            "{\"nome\":\"Bruno\",\"email\":\"bruno@exemplo.com\",\"perfil\":\"TECNICO\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TecnicoService service;

    @Test
    void criarDeveRetornar201ComLocation() throws Exception {
        given(service.criar(any(TecnicoRequest.class)))
                .willReturn(new TecnicoResponse(1L, "Bruno", "bruno@exemplo.com", Perfil.TECNICO));

        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/tecnicos/1"))
                .andExpect(jsonPath("$.perfil").value("TECNICO"));
    }

    @Test
    void criarSemPerfilDeveRetornar400() throws Exception {
        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bruno\",\"email\":\"bruno@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasItem("perfil: Perfil é obrigatório (ADMIN ou TECNICO)")));
    }

    @Test
    void criarComEmailDuplicadoDeveRetornar409() throws Exception {
        given(service.criar(any(TecnicoRequest.class)))
                .willThrow(new RegraNegocioException("Já existe técnico com o e-mail bruno@exemplo.com"));

        mockMvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void buscarInexistenteDeveRetornar404() throws Exception {
        given(service.buscar(99L)).willThrow(new RecursoNaoEncontradoException("Técnico 99 não encontrado"));

        mockMvc.perform(get("/tecnicos/99"))
                .andExpect(status().isNotFound());
    }
}
