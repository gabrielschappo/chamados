const mensagem = document.getElementById('mensagem');

function indicador(href, valor, rotulo) {
    return `<a class="indicador" href="${href}"><div class="valor">${valor}</div><div class="rotulo">${esc(rotulo)}</div></a>`;
}

async function contarChamados(status) {
    const filtro = status ? `&status=${status}` : '';
    const resposta = await api.get(`/chamados?size=1${filtro}`);
    return resposta.page.totalElements;
}

async function carregarIndicadores() {
    const statusLista = Object.keys(STATUS);
    const [total, ...porStatus] = await Promise.all([
        contarChamados(null), ...statusLista.map(contarChamados),
    ]);
    document.getElementById('indicadores-status').innerHTML =
        indicador('chamados.html', total, 'Total de chamados')
        + statusLista.map((s, i) => indicador(`chamados.html?status=${s}`, porStatus[i], STATUS[s])).join('');
}

async function carregarCadastros() {
    const [clientes, categorias, tecnicos] = await Promise.all([
        api.get('/clientes'), api.get('/categorias'), api.get('/tecnicos'),
    ]);
    document.getElementById('indicadores-cadastros').innerHTML =
        indicador('clientes.html', clientes.length, 'Clientes')
        + indicador('categorias.html', categorias.length, 'Categorias')
        + indicador('tecnicos.html', tecnicos.length, 'Técnicos');
}

async function carregarUltimos() {
    const resposta = await api.get('/chamados?size=5&sort=dataAbertura,desc');
    const corpo = document.getElementById('ultimos');
    if (resposta.content.length === 0) {
        corpo.innerHTML = '<tr><td colspan="4" class="vazio">Nenhum chamado aberto ainda.</td></tr>';
        return;
    }
    corpo.innerHTML = resposta.content.map(c => `
        <tr>
            <td>${c.id}</td>
            <td><a href="chamado.html?id=${c.id}">${esc(c.titulo)}</a></td>
            <td>${etiqueta('status', c.status, STATUS)}</td>
            <td>${formatarData(c.dataAbertura)}</td>
        </tr>`).join('');
}

Promise.all([carregarIndicadores(), carregarCadastros(), carregarUltimos()])
    .catch(e => mostrarErro(mensagem, e));
