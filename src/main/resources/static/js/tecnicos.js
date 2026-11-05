const form = document.getElementById('form-tecnico');
const campos = {
    id: document.getElementById('id'),
    nome: document.getElementById('nome'),
    email: document.getElementById('email'),
    perfil: document.getElementById('perfil'),
};
const tituloForm = document.getElementById('titulo-form');
const botaoCancelar = document.getElementById('cancelar');
const mensagemForm = document.getElementById('mensagem-form');
const mensagemLista = document.getElementById('mensagem-lista');
const lista = document.getElementById('lista');

let tecnicos = [];

preencherSelect(campos.perfil, PERFIS, 'Selecione...');

async function carregar() {
    try {
        tecnicos = await api.get('/tecnicos');
        renderizar();
    } catch (e) {
        mostrarErro(mensagemLista, e);
    }
}

function renderizar() {
    if (tecnicos.length === 0) {
        lista.innerHTML = '<tr><td colspan="5" class="vazio">Nenhum técnico cadastrado.</td></tr>';
        return;
    }
    lista.innerHTML = tecnicos.map(t => `
        <tr>
            <td>${t.id}</td>
            <td>${esc(t.nome)}</td>
            <td>${esc(t.email)}</td>
            <td>${etiqueta('perfil', t.perfil, PERFIS)}</td>
            <td class="acoes-linha">
                <button class="secundario pequeno" data-editar="${t.id}">Editar</button>
                <button class="perigo pequeno" data-excluir="${t.id}">Excluir</button>
            </td>
        </tr>`).join('');
}

function editar(id) {
    const t = tecnicos.find(x => x.id === id);
    campos.id.value = t.id;
    campos.nome.value = t.nome;
    campos.email.value = t.email;
    campos.perfil.value = t.perfil;
    tituloForm.textContent = `Editar técnico #${t.id}`;
    botaoCancelar.hidden = false;
    limparMensagem(mensagemForm);
    campos.nome.focus();
}

function limparFormulario() {
    form.reset();
    campos.id.value = '';
    tituloForm.textContent = 'Novo técnico';
    botaoCancelar.hidden = true;
}

async function excluir(id) {
    const t = tecnicos.find(x => x.id === id);
    if (!confirm(`Excluir o técnico "${t.nome}"?`)) return;
    try {
        await api.excluir(`/tecnicos/${id}`);
        mostrarSucesso(mensagemLista, 'Técnico excluído.');
        if (campos.id.value === String(id)) limparFormulario();
        carregar();
    } catch (e) {
        mostrarErro(mensagemLista, e);
    }
}

form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const dados = {
        nome: campos.nome.value.trim(),
        email: campos.email.value.trim(),
        perfil: valorOuNulo(campos.perfil.value),
    };
    try {
        if (campos.id.value) {
            await api.put(`/tecnicos/${campos.id.value}`, dados);
            mostrarSucesso(mensagemForm, 'Técnico atualizado.');
        } else {
            await api.post('/tecnicos', dados);
            mostrarSucesso(mensagemForm, 'Técnico cadastrado.');
        }
        limparFormulario();
        carregar();
    } catch (e) {
        mostrarErro(mensagemForm, e);
    }
});

botaoCancelar.addEventListener('click', () => {
    limparFormulario();
    limparMensagem(mensagemForm);
});

lista.addEventListener('click', (evento) => {
    const botao = evento.target.closest('button');
    if (!botao) return;
    if (botao.dataset.editar) editar(Number(botao.dataset.editar));
    if (botao.dataset.excluir) excluir(Number(botao.dataset.excluir));
});

carregar();
