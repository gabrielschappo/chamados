const form = document.getElementById('form-cliente');
const campos = {
    id: document.getElementById('id'),
    nome: document.getElementById('nome'),
    email: document.getElementById('email'),
    telefone: document.getElementById('telefone'),
};
const tituloForm = document.getElementById('titulo-form');
const botaoCancelar = document.getElementById('cancelar');
const mensagemForm = document.getElementById('mensagem-form');
const mensagemLista = document.getElementById('mensagem-lista');
const lista = document.getElementById('lista');

let clientes = [];

async function carregar() {
    try {
        clientes = await api.get('/clientes');
        renderizar();
    } catch (e) {
        mostrarErro(mensagemLista, e);
    }
}

function renderizar() {
    if (clientes.length === 0) {
        lista.innerHTML = '<tr><td colspan="5" class="vazio">Nenhum cliente cadastrado.</td></tr>';
        return;
    }
    lista.innerHTML = clientes.map(c => `
        <tr>
            <td>${c.id}</td>
            <td>${esc(c.nome)}</td>
            <td>${esc(c.email)}</td>
            <td>${esc(c.telefone) || '—'}</td>
            <td class="acoes-linha">
                <a class="botao secundario pequeno" href="chamados.html?clienteId=${c.id}">Chamados</a>
                <button class="secundario pequeno" data-editar="${c.id}">Editar</button>
                <button class="perigo pequeno" data-excluir="${c.id}">Excluir</button>
            </td>
        </tr>`).join('');
}

function editar(id) {
    const c = clientes.find(x => x.id === id);
    campos.id.value = c.id;
    campos.nome.value = c.nome;
    campos.email.value = c.email;
    campos.telefone.value = c.telefone || '';
    tituloForm.textContent = `Editar cliente #${c.id}`;
    botaoCancelar.hidden = false;
    limparMensagem(mensagemForm);
    campos.nome.focus();
}

function limparFormulario() {
    form.reset();
    campos.id.value = '';
    tituloForm.textContent = 'Novo cliente';
    botaoCancelar.hidden = true;
}

async function excluir(id) {
    const c = clientes.find(x => x.id === id);
    if (!confirm(`Excluir o cliente "${c.nome}"?`)) return;
    try {
        await api.excluir(`/clientes/${id}`);
        mostrarSucesso(mensagemLista, 'Cliente excluído.');
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
        telefone: valorOuNulo(campos.telefone.value.trim()),
    };
    try {
        if (campos.id.value) {
            await api.put(`/clientes/${campos.id.value}`, dados);
            mostrarSucesso(mensagemForm, 'Cliente atualizado.');
        } else {
            await api.post('/clientes', dados);
            mostrarSucesso(mensagemForm, 'Cliente cadastrado.');
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
