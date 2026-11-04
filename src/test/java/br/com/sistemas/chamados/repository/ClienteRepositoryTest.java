package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Cliente;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    @Test
    void deveSalvarClienteEGerarId() {
        Cliente salvo = repository.save(new Cliente("Ana", "ana@exemplo.com", "47999999999"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId())).isPresent();
    }

    @Test
    void existsByEmailDeveIndicarSeEmailJaFoiCadastrado() {
        repository.save(new Cliente("Ana", "ana@exemplo.com", null));

        assertThat(repository.existsByEmail("ana@exemplo.com")).isTrue();
        assertThat(repository.existsByEmail("outro@exemplo.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotDeveIgnorarOProprioCliente() {
        Cliente ana = repository.save(new Cliente("Ana", "ana@exemplo.com", null));
        Cliente beto = repository.save(new Cliente("Beto", "beto@exemplo.com", null));

        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", ana.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", beto.getId())).isTrue();
    }

    @Test
    void bancoDeveRejeitarEmailDuplicado() {
        repository.saveAndFlush(new Cliente("Ana", "ana@exemplo.com", null));

        assertThatThrownBy(() -> repository.saveAndFlush(new Cliente("Outra", "ana@exemplo.com", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
