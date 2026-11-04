package br.com.sistemas.chamados.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.exception.TransicaoStatusInvalidaException;
import br.com.sistemas.chamados.service.ChamadoService;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerTest {

    private static final String JSON_VALIDO =
            "{\"titulo\":\"PC não liga\",\"descricao\":\"Não liga\",\"prioridade\":\"ALTA\","
            + "\"clienteId\":1,\"categoriaId\":1}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService service;

    @Test
    void criarDeveRetornar201ComStatusAberto() throws Exception {
        given(service.criar(any(ChamadoRequest.class))).willReturn(chamado(1L, StatusChamado.ABERTO));

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/chamados/1"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.dataAbertura").exists());
    }

    @Test
    void criarSemCamposObrigatoriosDeveRetornar400() throws Exception {
        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Dados inválidos"))
                .andExpect(jsonPath("$.detalhes", hasSize(5)));
    }

    @Test
    void criarComClienteInexistenteDeveRetornar404() throws Exception {
        given(service.criar(any(ChamadoRequest.class)))
                .willThrow(new RecursoNaoEncontradoException("Cliente 1 não encontrado"));

        mockMvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Cliente 1 não encontrado"));
    }

    @Test
    void buscarInexistenteDeveRetornar404() throws Exception {
        given(service.buscar(99L)).willThrow(new RecursoNaoEncontradoException("Chamado 99 não encontrado"));

        mockMvc.perform(get("/chamados/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarDeveRepassarFiltrosEPaginacaoAoService() throws Exception {
        given(service.listar(any(), any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(chamado(1L, StatusChamado.ABERTO)), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/chamados")
                        .param("status", "ABERTO")
                        .param("prioridade", "ALTA")
                        .param("clienteId", "1")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "titulo,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(service).should().listar(eq(StatusChamado.ABERTO), eq(Prioridade.ALTA), eq(1L), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "titulo"));
    }

    @Test
    void listarSemParametrosDeveUsarPaginacaoPadrao() throws Exception {
        given(service.listar(any(), any(), any(), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/chamados")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(service).should().listar(isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "dataAbertura"));
    }

    @Test
    void listarComStatusInvalidoDeveRetornar400() throws Exception {
        mockMvc.perform(get("/chamados").param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]").value("status: valor 'XYZ' não é válido"));
    }

    @Test
    void alterarStatusDeveRetornar200() throws Exception {
        given(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
                .willReturn(chamado(1L, StatusChamado.EM_ATENDIMENTO));

        mockMvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_ATENDIMENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
    }

    @Test
    void alterarStatusComTransicaoInvalidaDeveRetornar409() throws Exception {
        given(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
                .willThrow(new TransicaoStatusInvalidaException(
                        "Não é permitido mudar o status de ABERTO para AGUARDANDO_CLIENTE"));

        mockMvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"AGUARDANDO_CLIENTE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é permitido mudar o status de ABERTO para AGUARDANDO_CLIENTE"));
    }

    @Test
    void alterarStatusSemStatusDeveRetornar400() throws Exception {
        mockMvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atribuirEmChamadoFechadoDeveRetornar409() throws Exception {
        given(service.atribuir(eq(1L), any(AtribuirTecnicoRequest.class)))
                .willThrow(new RegraNegocioException("Chamado 1 está fechado e não pode ser alterado"));

        mockMvc.perform(patch("/chamados/1/atribuir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Chamado 1 está fechado e não pode ser alterado"));
    }

    @Test
    void atribuirSemTecnicoDeveRetornar400() throws Exception {
        mockMvc.perform(patch("/chamados/1/atribuir").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static ChamadoResponse chamado(Long id, StatusChamado status) {
        return new ChamadoResponse(id, "PC não liga", "Não liga", LocalDateTime.now(), null,
                Prioridade.ALTA, status, 1L, "Ana", 1L, "Hardware", null, null);
    }
}
