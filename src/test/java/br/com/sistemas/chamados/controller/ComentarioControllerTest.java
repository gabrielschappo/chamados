package br.com.sistemas.chamados.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.service.ComentarioService;

@WebMvcTest(ComentarioController.class)
class ComentarioControllerTest {

    private static final String JSON_VALIDO = "{\"texto\":\"Verificando fonte\",\"autor\":\"Bruno\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComentarioService service;

    @Test
    void criarDeveRetornar201ComLocation() throws Exception {
        given(service.criar(eq(1L), any(ComentarioRequest.class)))
                .willReturn(new ComentarioResponse(5L, "Verificando fonte", LocalDateTime.now(), "Bruno", 1L));

        mockMvc.perform(post("/chamados/1/comentarios").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/chamados/1/comentarios/5"))
                .andExpect(jsonPath("$.chamadoId").value(1));
    }

    @Test
    void criarSemTextoEAutorDeveRetornar400() throws Exception {
        mockMvc.perform(post("/chamados/1/comentarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasSize(2)));
    }

    @Test
    void criarEmChamadoInexistenteDeveRetornar404() throws Exception {
        given(service.criar(eq(99L), any(ComentarioRequest.class)))
                .willThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(post("/chamados/99/comentarios").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarDeveRetornar200() throws Exception {
        given(service.listar(1L)).willReturn(List.of(
                new ComentarioResponse(1L, "Verificando", LocalDateTime.now(), "Bruno", 1L)));

        mockMvc.perform(get("/chamados/1/comentarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
