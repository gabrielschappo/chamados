package br.com.sistemas.chamados.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    private static final String JSON_VALIDO =
            "{\"nome\":\"Ana Souza\",\"email\":\"ana@exemplo.com\",\"telefone\":\"47999999999\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService service;
    @MockitoBean
    private ChamadoService chamadoService;

    @Test
    void criarDeveRetornar201ComLocation() throws Exception {
        given(service.criar(any(ClienteRequest.class)))
                .willReturn(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", "47999999999"));

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/clientes/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@exemplo.com"));
    }

    @Test
    void criarComDadosInvalidosDeveRetornar400() throws Exception {
        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"email-invalido\",\"telefone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                .andExpect(jsonPath("$.detalhes", hasSize(2)))
                .andExpect(jsonPath("$.detalhes", hasItems("nome: Nome é obrigatório", "email: E-mail inválido")));
    }

    @Test
    void criarComEmailDuplicadoDeveRetornar409() throws Exception {
        given(service.criar(any(ClienteRequest.class)))
                .willThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Já existe cliente com o e-mail ana@exemplo.com"));
    }

    @Test
    void listarDeveRetornar200() throws Exception {
        given(service.listar()).willReturn(List.of(new ClienteResponse(1L, "Ana", "ana@exemplo.com", null)));

        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void buscarInexistenteDeveRetornar404() throws Exception {
        given(service.buscar(99L)).willThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mockMvc.perform(get("/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Cliente 99 não encontrado"));
    }

    @Test
    void buscarComIdNaoNumericoDeveRetornar400() throws Exception {
        mockMvc.perform(get("/clientes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido"));
    }

    @Test
    void atualizarComEmailDeOutroClienteDeveRetornar409() throws Exception {
        given(service.atualizar(eq(1L), any(ClienteRequest.class)))
                .willThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mockMvc.perform(put("/clientes/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void excluirDeveRetornar204() throws Exception {
        mockMvc.perform(delete("/clientes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluirInexistenteDeveRetornar404() throws Exception {
        willThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado")).given(service).excluir(99L);

        mockMvc.perform(delete("/clientes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarChamadosDeClienteInexistenteDeveRetornar404() throws Exception {
        given(chamadoService.listarPorCliente(eq(99L), any(Pageable.class)))
                .willThrow(new RecursoNaoEncontradoException("Cliente 99 não encontrado"));

        mockMvc.perform(get("/clientes/99/chamados"))
                .andExpect(status().isNotFound());
    }
}
