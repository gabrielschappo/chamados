package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Categoria;

@DataJpaTest
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository repository;

    @Test
    void deveSalvarCategoriaEGerarId() {
        Categoria salva = repository.save(new Categoria("Hardware", "Equipamentos"));

        assertThat(salva.getId()).isNotNull();
    }

    @Test
    void existsByNomeDeveIndicarSeNomeJaFoiCadastrado() {
        repository.save(new Categoria("Hardware", null));

        assertThat(repository.existsByNome("Hardware")).isTrue();
        assertThat(repository.existsByNome("Rede")).isFalse();
    }

    @Test
    void existsByNomeAndIdNotDeveIgnorarAPropriaCategoria() {
        Categoria hardware = repository.save(new Categoria("Hardware", null));
        Categoria rede = repository.save(new Categoria("Rede", null));

        assertThat(repository.existsByNomeAndIdNot("Hardware", hardware.getId())).isFalse();
        assertThat(repository.existsByNomeAndIdNot("Hardware", rede.getId())).isTrue();
    }

    @Test
    void bancoDeveRejeitarNomeDuplicado() {
        repository.saveAndFlush(new Categoria("Hardware", null));

        assertThatThrownBy(() -> repository.saveAndFlush(new Categoria("Hardware", "outra")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
