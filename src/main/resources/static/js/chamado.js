const id = parametroUrl('id');

const el = (idElemento) => document.getElementById(idElemento);
const mensagemPagina = el('mensagem-pagina');
const editar = {
    titulo: el('editar-titulo'),
    descricao: el('editar-descricao'),
    prioridade: el('editar-prioridade'),
    cliente: el('editar-cliente'),
    categoria: el('editar-categoria'),
    tecnico: el('editar-tecnico'),
};

let chamado = null;
let tecnicos = [];

function porId(itens) {
    return Object.fromEntries(itens.map(i => [i.id, i.nome]));
}

async function carregarOpcoes() {
    const [clientes, categorias, listaTecnicos] = await Promise.all([
        api.get('/clientes'), api.get('/categorias'), api.get('/tecnicos'),
    ]);
    tecnicos = listaTecnicos;
    preencherSelect(editar.prioridade, PRIORIDADES);
    preencherSelect(editar.cliente, porId(clientes));
    preencherSelect(editar.categoria, porId(categorias));
    preencherSelect(editar.tecnico, porId(tecnicos), 'Sem técnico');
    preencherSelect(el('novo-tecnico'), porId(tecnicos), 'Selecione...');
}

async function carregarChamado() {
    chamado = await api.get(`/chamados/${id}`);
    renderizar();
}

function renderizar() {
    const c = chamado;
    document.title = `#${c.id} ${c.titulo} · Sistema de Chamados`;
    el('titulo').textContent = c.titulo;
    el('status').innerHTML = etiqueta('status', c.status, STATUS);
    el('prioridade').innerHTML = etiqueta('prioridade', c.prioridade, PRIORIDADES);
    el('numero').textContent = `#${c.id}`;
    el('cliente').innerHTML = `<a href="chamados.html?clienteId=${c.clienteId}">${esc(c.clienteNome)}</a>`;
    el('categoria').textContent = c.categoriaNome;
    el('tecnico').textContent = c.tecnicoNome || 'Não atribuído';
    el('abertura').textContent = formatarData(c.dataAbertura);
    el('fechamento').textContent = formatarData(c.dataFechamento);
    el('descricao').textContent = c.descricao;

    const proximos = Object.fromEntries(TRANSICOES[c.status].map(s => [s, STATUS[s]]));
    preencherSelect(el('novo-status'), proximos);

    el('novo-tecnico').value = c.tecnicoId || '';

    editar.titulo.value = c.titulo;
    editar.descricao.value = c.descricao;
    editar.prioridade.value = c.prioridade;
    editar.cliente.value = c.clienteId;
    editar.categoria.value = c.categoriaId;
    editar.tecnico.value = c.tecnicoId || '';

    const fechado = c.status === 'FECHADO';
    el('aviso-fechado').hidden = !fechado;
    el('acoes-chamado').hidden = fechado;
    el('secao-editar').hidden = fechado;

    renderizarComentarios(c.comentarios);
}

function renderizarComentarios(comentarios) {
    const lista = el('comentarios');
    if (comentarios.length === 0) {
        lista.innerHTML = '<li class="vazio">Nenhum comentário ainda.</li>';
        return;
    }
    lista.innerHTML = comentarios.map(co => `
        <li>
            <div class="meta"><strong>${esc(co.autor)}</strong> · ${formatarData(co.data)}</div>
            <div class="texto">${esc(co.texto)}</div>
        </li>`).join('');
}

/** Executa uma ação na API, recarrega o chamado e mostra o resultado no elemento informado. */
async function executar(mensagem, acao, textoSucesso) {
    try {
        await acao();
        await carregarChamado();
        mostrarSucesso(mensagem, textoSucesso);
    } catch (e) {
        mostrarErro(mensagem, e);
    }
}

el('form-status').addEventListener('submit', (evento) => {
    evento.preventDefault();
    const status = el('novo-status').value;
    if (status === 'FECHADO' && !confirm('Fechar o chamado? Depois de fechado ele não poderá mais ser alterado.')) return;
    executar(el('mensagem-status'),
        () => api.patch(`/chamados/${id}/status`, { status }),
        `Status alterado para "${STATUS[status]}".`);
});

el('form-atribuir').addEventListener('submit', (evento) => {
    evento.preventDefault();
    const tecnicoId = idOuNulo(el('novo-tecnico').value);
    executar(el('mensagem-atribuir'),
        () => api.patch(`/chamados/${id}/atribuir`, { tecnicoId }),
        'Técnico atribuído.');
});

el('form-comentario').addEventListener('submit', (evento) => {
    evento.preventDefault();
    const dados = { autor: el('autor').value.trim(), texto: el('texto').value.trim() };
    executar(el('mensagem-comentario'),
        async () => {
            await api.post(`/chamados/${id}/comentarios`, dados);
            el('texto').value = '';
        },
        'Comentário adicionado.');
});

el('form-editar').addEventListener('submit', (evento) => {
    evento.preventDefault();
    const dados = {
        titulo: editar.titulo.value.trim(),
        descricao: editar.descricao.value.trim(),
        prioridade: editar.prioridade.value,
        clienteId: idOuNulo(editar.cliente.value),
        categoriaId: idOuNulo(editar.categoria.value),
        tecnicoId: idOuNulo(editar.tecnico.value),
    };
    executar(el('mensagem-editar'), () => api.put(`/chamados/${id}`, dados), 'Chamado atualizado.');
});

el('excluir').addEventListener('click', async () => {
    if (!confirm(`Excluir o chamado #${id} e todos os seus comentários?`)) return;
    try {
        await api.excluir(`/chamados/${id}`);
        window.location.href = 'chamados.html';
    } catch (e) {
        mostrarErro(mensagemPagina, e);
    }
});

(async function iniciar() {
    if (!id) {
        mostrarErro(mensagemPagina, new ApiError(400, { erro: 'Nenhum chamado informado na URL.' }));
        return;
    }
    try {
        await carregarOpcoes();
        await carregarChamado();
        el('conteudo').hidden = false;
    } catch (e) {
        mostrarErro(mensagemPagina, e);
    }
})();
