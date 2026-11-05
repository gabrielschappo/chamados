const TAMANHO_PAGINA = 10;

const formChamado = document.getElementById('form-chamado');
const novo = {
    titulo: document.getElementById('titulo'),
    descricao: document.getElementById('descricao'),
    prioridade: document.getElementById('prioridade'),
    cliente: document.getElementById('cliente'),
    categoria: document.getElementById('categoria'),
    tecnico: document.getElementById('tecnico'),
};
const filtros = {
    status: document.getElementById('filtro-status'),
    prioridade: document.getElementById('filtro-prioridade'),
    cliente: document.getElementById('filtro-cliente'),
    ordenacao: document.getElementById('ordenacao'),
};
const mensagemForm = document.getElementById('mensagem-form');
const mensagemLista = document.getElementById('mensagem-lista');
const lista = document.getElementById('lista');
const infoPagina = document.getElementById('info-pagina');
const botaoAnterior = document.getElementById('anterior');
const botaoProxima = document.getElementById('proxima');

let paginaAtual = 0;

function porId(itens) {
    return Object.fromEntries(itens.map(i => [i.id, i.nome]));
}

async function carregarOpcoes() {
    preencherSelect(novo.prioridade, PRIORIDADES, 'Selecione...');
    preencherSelect(filtros.status, STATUS, 'Todos');
    preencherSelect(filtros.prioridade, PRIORIDADES, 'Todas');
    try {
        const [clientes, categorias, tecnicos] = await Promise.all([
            api.get('/clientes'), api.get('/categorias'), api.get('/tecnicos'),
        ]);
        preencherSelect(novo.cliente, porId(clientes), 'Selecione...');
        preencherSelect(novo.categoria, porId(categorias), 'Selecione...');
        preencherSelect(novo.tecnico, porId(tecnicos), 'Sem técnico');
        preencherSelect(filtros.cliente, porId(clientes), 'Todos');

        if (clientes.length === 0 || categorias.length === 0) {
            mensagemForm.className = 'mensagem aviso';
            mensagemForm.innerHTML = 'Para abrir um chamado, cadastre antes pelo menos um '
                + '<a href="clientes.html">cliente</a> e uma <a href="categorias.html">categoria</a>.';
        }
    } catch (e) {
        mostrarErro(mensagemLista, e);
    }
}

function aplicarFiltrosDaUrl() {
    filtros.status.value = parametroUrl('status') || '';
    filtros.prioridade.value = parametroUrl('prioridade') || '';
    filtros.cliente.value = parametroUrl('clienteId') || '';
}

function atualizarUrl() {
    const params = new URLSearchParams();
    if (filtros.status.value) params.set('status', filtros.status.value);
    if (filtros.prioridade.value) params.set('prioridade', filtros.prioridade.value);
    if (filtros.cliente.value) params.set('clienteId', filtros.cliente.value);
    const query = params.toString();
    history.replaceState(null, '', query ? `?${query}` : location.pathname);
}

async function carregarChamados() {
    const params = new URLSearchParams({
        page: paginaAtual,
        size: TAMANHO_PAGINA,
        sort: filtros.ordenacao.value,
    });
    if (filtros.status.value) params.set('status', filtros.status.value);
    if (filtros.prioridade.value) params.set('prioridade', filtros.prioridade.value);
    if (filtros.cliente.value) params.set('clienteId', filtros.cliente.value);

    try {
        const resposta = await api.get(`/chamados?${params}`);
        limparMensagem(mensagemLista);
        renderizar(resposta);
    } catch (e) {
        mostrarErro(mensagemLista, e);
    }
}

function renderizar({ content, page }) {
    if (content.length === 0) {
        lista.innerHTML = '<tr><td colspan="9" class="vazio">Nenhum chamado encontrado.</td></tr>';
    } else {
        lista.innerHTML = content.map(c => `
            <tr>
                <td>${c.id}</td>
                <td><a href="chamado.html?id=${c.id}">${esc(c.titulo)}</a></td>
                <td>${esc(c.clienteNome)}</td>
                <td>${esc(c.categoriaNome)}</td>
                <td>${etiqueta('prioridade', c.prioridade, PRIORIDADES)}</td>
                <td>${etiqueta('status', c.status, STATUS)}</td>
                <td>${esc(c.tecnicoNome) || '—'}</td>
                <td>${formatarData(c.dataAbertura)}</td>
                <td class="acoes-linha"><a class="botao secundario pequeno" href="chamado.html?id=${c.id}">Abrir</a></td>
            </tr>`).join('');
    }
    const totalPaginas = Math.max(page.totalPages, 1);
    infoPagina.textContent = `Página ${page.number + 1} de ${totalPaginas} · ${page.totalElements} chamado(s)`;
    botaoAnterior.disabled = page.number === 0;
    botaoProxima.disabled = page.number + 1 >= page.totalPages;
}

formChamado.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const dados = {
        titulo: novo.titulo.value.trim(),
        descricao: novo.descricao.value.trim(),
        prioridade: valorOuNulo(novo.prioridade.value),
        clienteId: idOuNulo(novo.cliente.value),
        categoriaId: idOuNulo(novo.categoria.value),
        tecnicoId: idOuNulo(novo.tecnico.value),
    };
    try {
        const criado = await api.post('/chamados', dados);
        window.location.href = `chamado.html?id=${criado.id}`;
    } catch (e) {
        mostrarErro(mensagemForm, e);
    }
});

Object.values(filtros).forEach(select => select.addEventListener('change', () => {
    paginaAtual = 0;
    atualizarUrl();
    carregarChamados();
}));

botaoAnterior.addEventListener('click', () => { paginaAtual--; carregarChamados(); });
botaoProxima.addEventListener('click', () => { paginaAtual++; carregarChamados(); });

(async function iniciar() {
    await carregarOpcoes();
    aplicarFiltrosDaUrl();
    if (parametroUrl('novo') !== null) {
        document.getElementById('novo-chamado').open = true;
    }
    carregarChamados();
})();
