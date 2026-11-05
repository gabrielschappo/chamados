class ApiError extends Error {
    constructor(status, corpo) {
        super((corpo && (corpo.erro || corpo.error)) || `Erro ${status}`);
        this.status = status;
        this.detalhes = (corpo && corpo.detalhes) || [];
    }
}

const api = {
    async request(metodo, url, corpo) {
        const opcoes = { method: metodo, headers: {} };
        if (corpo !== undefined) {
            opcoes.headers['Content-Type'] = 'application/json';
            opcoes.body = JSON.stringify(corpo);
        }
        let resposta;
        try {
            resposta = await fetch(url, opcoes);
        } catch (e) {
            throw new ApiError(0, { erro: 'Não foi possível conectar à API. A aplicação está rodando?' });
        }
        if (resposta.status === 204) {
            return null;
        }
        const dados = await resposta.json().catch(() => null);
        if (!resposta.ok) {
            throw new ApiError(resposta.status, dados);
        }
        return dados;
    },
    get(url) { return this.request('GET', url); },
    post(url, corpo) { return this.request('POST', url, corpo); },
    put(url, corpo) { return this.request('PUT', url, corpo); },
    patch(url, corpo) { return this.request('PATCH', url, corpo); },
    excluir(url) { return this.request('DELETE', url); },
};

const STATUS = {
    ABERTO: 'Aberto',
    EM_ATENDIMENTO: 'Em atendimento',
    AGUARDANDO_CLIENTE: 'Aguardando cliente',
    FECHADO: 'Fechado',
};

const PRIORIDADES = {
    BAIXA: 'Baixa',
    MEDIA: 'Média',
    ALTA: 'Alta',
    CRITICA: 'Crítica',
};

const PERFIS = {
    ADMIN: 'Administrador',
    TECNICO: 'Técnico',
};

const TRANSICOES = {
    ABERTO: ['EM_ATENDIMENTO', 'FECHADO'],
    EM_ATENDIMENTO: ['AGUARDANDO_CLIENTE', 'FECHADO'],
    AGUARDANDO_CLIENTE: ['EM_ATENDIMENTO', 'FECHADO'],
    FECHADO: [],
};

function esc(valor) {
    if (valor === null || valor === undefined) return '';
    return String(valor)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function etiqueta(tipo, valor, rotulos) {
    return `<span class="etiqueta ${tipo}-${esc(valor)}">${esc(rotulos[valor] || valor)}</span>`;
}

function formatarData(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function mostrarErro(elemento, erro) {
    elemento.className = 'mensagem erro';
    const detalhes = erro.detalhes && erro.detalhes.length
        ? '<ul>' + erro.detalhes.map(d => `<li>${esc(d)}</li>`).join('') + '</ul>'
        : '';
    elemento.innerHTML = `<strong>${esc(erro.message)}</strong>${detalhes}`;
}

function mostrarSucesso(elemento, texto) {
    elemento.className = 'mensagem sucesso';
    elemento.textContent = texto;
}

function limparMensagem(elemento) {
    elemento.className = 'mensagem';
    elemento.innerHTML = '';
}

function preencherSelect(select, opcoes, textoVazio) {
    const vazio = textoVazio !== undefined ? `<option value="">${esc(textoVazio)}</option>` : '';
    select.innerHTML = vazio + Object.entries(opcoes)
        .map(([valor, rotulo]) => `<option value="${esc(valor)}">${esc(rotulo)}</option>`)
        .join('');
}

function parametroUrl(nome) {
    return new URLSearchParams(window.location.search).get(nome);
}

function valorOuNulo(valor) {
    return valor === '' || valor === undefined ? null : valor;
}

function idOuNulo(valor) {
    return valor === '' || valor === undefined || valor === null ? null : Number(valor);
}
