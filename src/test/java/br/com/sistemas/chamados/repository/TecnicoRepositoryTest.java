package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Tecnico;

@DataJpaTest
class TecnicoRepositoryTest {

    @Autowired
    private TecnicoRepository repository;

    @Test
    void deveSalvarTecnicoComPerfil() {
        Tecnico salvo = repository.saveAndFlush(new Tecnico("Bruno", "bruno@exemplo.com", Perfil.ADMIN));

        assertThat(repository.findById(salvo.getId()))
                .get()
                .extracting(Tecnico::getPerfil)
                .isEqualTo(Perfil.ADMIN);
    }

    @Test
    void existsByEmailDeveIndicarSeEmailJaFoiCadastrado() {
        repository.save(new Tecnico("Bruno", "bruno@exemplo.com", Perfil.TECNICO));

        assertThat(repository.existsByEmail("bruno@exemplo.com")).isTrue();
        assertThat(repository.existsByEmail("outro@exemplo.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotDeveIgnorarOProprioTecnico() {
        Tecnico bruno = repository.save(new Tecnico("Bruno", "bruno@exemplo.com", Perfil.TECNICO));
        Tecnico carla = repository.save(new Tecnico("Carla", "carla@exemplo.com", Perfil.TECNICO));

        assertThat(repository.existsByEmailAndIdNot("bruno@exemplo.com", bruno.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("bruno@exemplo.com", carla.getId())).isTrue();
    }

    @Test
    void bancoDeveRejeitarEmailDuplicado() {
        repository.saveAndFlush(new Tecnico("Bruno", "bruno@exemplo.com", Perfil.TECNICO));

        assertThatThrownBy(() -> repository.saveAndFlush(new Tecnico("Outro", "bruno@exemplo.com", Perfil.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
