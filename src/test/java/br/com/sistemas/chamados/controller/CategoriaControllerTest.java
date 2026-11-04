package br.com.sistemas.chamados.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;

@WebMvcTest(CategoriaController.class)
class CategoriaControllerTest {

    private static final String JSON_VALIDO = "{\"nome\":\"Hardware\",\"descricao\":\"Equipamentos\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService service;

    @Test
    void criarDeveRetornar201ComLocation() throws Exception {
        given(service.criar(any(CategoriaRequest.class)))
                .willReturn(new CategoriaResponse(1L, "Hardware", "Equipamentos"));

        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/categorias/1"))
                .andExpect(jsonPath("$.nome").value("Hardware"));
    }

    @Test
    void criarSemNomeDeveRetornar400() throws Exception {
        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasItem("nome: Nome é obrigatório")));
    }

    @Test
    void criarComNomeDuplicadoDeveRetornar409() throws Exception {
        given(service.criar(any(CategoriaRequest.class)))
                .willThrow(new RegraNegocioException("Já existe categoria com o nome Hardware"));

        mockMvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe categoria com o nome Hardware"));
    }

    @Test
    void buscarInexistenteDeveRetornar404() throws Exception {
        given(service.buscar(99L)).willThrow(new RecursoNaoEncontradoException("Categoria 99 não encontrada"));

        mockMvc.perform(get("/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Categoria 99 não encontrada"));
    }

    @Test
    void excluirCategoriaComChamadosDeveRetornar409() throws Exception {
        willThrow(new RegraNegocioException("Categoria 1 possui chamados e não pode ser excluída"))
                .given(service).excluir(1L);

        mockMvc.perform(delete("/categorias/1"))
                .andExpect(status().isConflict());
    }
}
