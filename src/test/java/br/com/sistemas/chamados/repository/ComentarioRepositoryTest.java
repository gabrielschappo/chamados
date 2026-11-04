package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Prioridade;

@DataJpaTest
class ComentarioRepositoryTest {

    @Autowired
    private ComentarioRepository repository;
    @Autowired
    private ChamadoRepository chamadoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private Chamado chamado1;
    private Chamado chamado2;

    @BeforeEach
    void setUp() {
        Cliente ana = clienteRepository.save(new Cliente("Ana", "ana@exemplo.com", null));
        Categoria hardware = categoriaRepository.save(new Categoria("Hardware", null));
        chamado1 = chamadoRepository.save(new Chamado("C1", "d", Prioridade.ALTA, ana, hardware, null));
        chamado2 = chamadoRepository.save(new Chamado("C2", "d", Prioridade.BAIXA, ana, hardware, null));

        repository.save(new Comentario("Verificando", "Bruno", chamado1));
        repository.save(new Comentario("Resolvido", "Bruno", chamado1));
        repository.save(new Comentario("Outro chamado", "Carla", chamado2));
    }

    @Test
    void comentarioDeveReceberDataAutomaticamente() {
        Comentario salvo = repository.saveAndFlush(new Comentario("Texto", "Autor", chamado1));

        assertThat(salvo.getData()).isNotNull();
    }

    @Test
    void findByChamadoIdDeveRetornarSomenteComentariosDoChamado() {
        assertThat(repository.findByChamadoIdOrderByDataAsc(chamado1.getId()))
                .extracting(Comentario::getTexto)
                .containsExactlyInAnyOrder("Verificando", "Resolvido");
    }

    @Test
    void deleteByChamadoIdDeveApagarSomenteComentariosDoChamado() {
        repository.deleteByChamadoId(chamado1.getId());

        assertThat(repository.findByChamadoIdOrderByDataAsc(chamado1.getId())).isEmpty();
        assertThat(repository.findByChamadoIdOrderByDataAsc(chamado2.getId())).hasSize(1);
    }
}
