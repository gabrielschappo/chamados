package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;

@DataJpaTest
class ChamadoRepositoryTest {

    private static final Pageable TODOS_POR_TITULO = PageRequest.of(0, 10, Sort.by("titulo"));

    @Autowired
    private ChamadoRepository repository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private TecnicoRepository tecnicoRepository;

    private Cliente ana;
    private Cliente beto;
    private Categoria hardware;
    private Tecnico bruno;

    @BeforeEach
    void setUp() {
        ana = clienteRepository.save(new Cliente("Ana", "ana@exemplo.com", null));
        beto = clienteRepository.save(new Cliente("Beto", "beto@exemplo.com", null));
        hardware = categoriaRepository.save(new Categoria("Hardware", null));
        bruno = tecnicoRepository.save(new Tecnico("Bruno", "bruno@exemplo.com", Perfil.TECNICO));

        salvar("C1", Prioridade.ALTA, ana, bruno, StatusChamado.EM_ATENDIMENTO);
        salvar("C2", Prioridade.BAIXA, ana, null, StatusChamado.ABERTO);
        salvar("C3", Prioridade.ALTA, beto, null, StatusChamado.ABERTO);
        salvar("C4", Prioridade.CRITICA, ana, null, StatusChamado.FECHADO);
        repository.flush();
    }

    @Test
    void chamadoDeveNascerAbertoComDataDeAbertura() {
        Chamado novo = repository.saveAndFlush(
                new Chamado("Novo", "descricao", Prioridade.MEDIA, ana, hardware, null));

        assertThat(novo.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(novo.getDataAbertura()).isNotNull();
        assertThat(novo.getDataFechamento()).isNull();
    }

    @Test
    void filtrarSemParametrosDeveRetornarTodos() {
        assertThat(titulos(repository.filtrar(null, null, null, TODOS_POR_TITULO)))
                .containsExactly("C1", "C2", "C3", "C4");
    }

    @Test
    void filtrarPorStatus() {
        assertThat(titulos(repository.filtrar(StatusChamado.ABERTO, null, null, TODOS_POR_TITULO)))
                .containsExactly("C2", "C3");
    }

    @Test
    void filtrarPorPrioridade() {
        assertThat(titulos(repository.filtrar(null, Prioridade.ALTA, null, TODOS_POR_TITULO)))
                .containsExactly("C1", "C3");
    }

    @Test
    void filtrarPorCliente() {
        assertThat(titulos(repository.filtrar(null, null, ana.getId(), TODOS_POR_TITULO)))
                .containsExactly("C1", "C2", "C4");
    }

    @Test
    void filtrarCombinandoStatusPrioridadeECliente() {
        assertThat(titulos(repository.filtrar(StatusChamado.ABERTO, Prioridade.ALTA, null, TODOS_POR_TITULO)))
                .containsExactly("C3");
        assertThat(titulos(repository.filtrar(StatusChamado.ABERTO, null, ana.getId(), TODOS_POR_TITULO)))
                .containsExactly("C2");
        assertThat(titulos(repository.filtrar(
                StatusChamado.EM_ATENDIMENTO, Prioridade.ALTA, ana.getId(), TODOS_POR_TITULO)))
                .containsExactly("C1");
        assertThat(repository.filtrar(StatusChamado.FECHADO, null, beto.getId(), TODOS_POR_TITULO))
                .isEmpty();
    }

    @Test
    void filtrarDevePaginar() {
        Page<Chamado> segundaPagina = repository.filtrar(null, null, null, PageRequest.of(1, 3, Sort.by("titulo")));

        assertThat(titulos(segundaPagina)).containsExactly("C4");
        assertThat(segundaPagina.getTotalElements()).isEqualTo(4);
        assertThat(segundaPagina.getTotalPages()).isEqualTo(2);
    }

    @Test
    void ordenarPorNomeDoTecnicoDeveManterChamadosSemTecnico() {
        Page<Chamado> pagina = repository.filtrar(null, null, null, PageRequest.of(0, 10, Sort.by("tecnico.nome")));

        assertThat(pagina.getTotalElements()).isEqualTo(4);
        assertThat(pagina.getContent()).hasSize(4);
    }

    @Test
    void existsByRelacionamentoDeveIndicarSeHaChamadosVinculados() {
        Cliente semChamados = clienteRepository.save(new Cliente("Caio", "caio@exemplo.com", null));

        assertThat(repository.existsByClienteId(ana.getId())).isTrue();
        assertThat(repository.existsByClienteId(semChamados.getId())).isFalse();
        assertThat(repository.existsByCategoriaId(hardware.getId())).isTrue();
        assertThat(repository.existsByTecnicoId(bruno.getId())).isTrue();
    }

    private void salvar(String titulo, Prioridade prioridade, Cliente cliente, Tecnico tecnico, StatusChamado status) {
        Chamado chamado = repository.save(new Chamado(titulo, "descricao", prioridade, cliente, hardware, tecnico));
        chamado.setStatus(status);
    }

    private static List<String> titulos(Page<Chamado> pagina) {
        return pagina.map(Chamado::getTitulo).getContent();
    }
}
