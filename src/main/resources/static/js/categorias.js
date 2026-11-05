const form = document.getElementById('form-categoria');
const campos = {
    id: document.getElementById('id'),
    nome: document.getElementById('nome'),
    descricao: document.getElementById('descricao'),
};
const tituloForm = document.getElementById('titulo-form');
const botaoCancelar = document.getElementById('cancelar');
const mensagemForm = document.getElementById('mensagem-form');
const mensagemLista = document.getElementById('mensagem-lista');
const lista = document.getElementById('lista');

let categorias = [];

async function carregar() {
    try {
        categorias = await api.get('/categorias');
        renderizar();
    } catch (e) {
        mostrarErro(mensagemLista, e);
    }
}

function renderizar() {
    if (categorias.length === 0) {
        lista.innerHTML = '<tr><td colspan="4" class="vazio">Nenhuma categoria cadastrada.</td></tr>';
        return;
    }
    lista.innerHTML = categorias.map(c => `
        <tr>
            <td>${c.id}</td>
            <td>${esc(c.nome)}</td>
            <td>${esc(c.descricao) || '—'}</td>
            <td class="acoes-linha">
                <button class="secundario pequeno" data-editar="${c.id}">Editar</button>
                <button class="perigo pequeno" data-excluir="${c.id}">Excluir</button>
            </td>
        </tr>`).join('');
}

function editar(id) {
    const c = categorias.find(x => x.id === id);
    campos.id.value = c.id;
    campos.nome.value = c.nome;
    campos.descricao.value = c.descricao || '';
    tituloForm.textContent = `Editar categoria #${c.id}`;
    botaoCancelar.hidden = false;
    limparMensagem(mensagemForm);
    campos.nome.focus();
}

function limparFormulario() {
    form.reset();
    campos.id.value = '';
    tituloForm.textContent = 'Nova categoria';
    botaoCancelar.hidden = true;
}

async function excluir(id) {
    const c = categorias.find(x => x.id === id);
    if (!confirm(`Excluir a categoria "${c.nome}"?`)) return;
    try {
        await api.excluir(`/categorias/${id}`);
        mostrarSucesso(mensagemLista, 'Categoria excluída.');
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
        descricao: valorOuNulo(campos.descricao.value.trim()),
    };
    try {
        if (campos.id.value) {
            await api.put(`/categorias/${campos.id.value}`, dados);
            mostrarSucesso(mensagemForm, 'Categoria atualizada.');
        } else {
            await api.post('/categorias', dados);
            mostrarSucesso(mensagemForm, 'Categoria cadastrada.');
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
