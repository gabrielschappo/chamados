# Sistema de Chamados

API REST de um sistema de chamados em arquitetura em camadas, desenvolvida na disciplina
DAS - Design e Arquitetura de Sistemas (UNIVILLE).

Tecnologias: Java 21, Spring Boot, Maven, Spring Web, Spring Data JPA, H2 e Bean Validation.

## Como executar

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

O banco H2 fica em memória e é recriado a cada reinício. Para consultar o banco, acesse
`http://localhost:8080/h2-console` com JDBC URL `jdbc:h2:mem:chamadosdb`, usuário `sa` e senha em branco.

## Telas (front-end)

Com a aplicação rodando, acesse `http://localhost:8080/`. As telas são páginas HTML + JavaScript em
`src/main/resources/static` e usam a mesma API REST documentada abaixo (via `fetch`). Não acessam o banco diretamente.

| Tela | Arquivo | O que faz |
|---|---|---|
| Início | `index.html` | Contadores por status, que levam à lista filtrada; totais de cadastros e últimos chamados |
| Chamados | `chamados.html` | Abrir chamado; lista com filtros (status, prioridade, cliente), ordenação e paginação |
| Detalhe do chamado | `chamado.html?id={id}` | Dados, alterar status, atribuir técnico, comentários, editar e excluir |
| Clientes | `clientes.html` | Cadastrar, editar, excluir e ver os chamados do cliente |
| Categorias | `categorias.html` | Cadastrar, editar e excluir |
| Técnicos | `tecnicos.html` | Cadastrar, editar e excluir |

As mensagens de erro da API (400, 404 e 409) aparecem na própria tela.

## Rotas

### Clientes

| Método | Rota | Ação | Sucesso |
|---|---|---|---|
| POST | `/clientes` | Criar cliente | 201 |
| GET | `/clientes` | Listar todos | 200 |
| GET | `/clientes/{id}` | Buscar por ID | 200 |
| GET | `/clientes/{id}/chamados` | Chamados do cliente (paginado) | 200 |
| PUT | `/clientes/{id}` | Atualizar | 200 |
| DELETE | `/clientes/{id}` | Excluir | 204 |

### Categorias e técnicos

| Método | Rota | Ação | Sucesso |
|---|---|---|---|
| POST | `/categorias` · `/tecnicos` | Criar | 201 |
| GET | `/categorias` · `/tecnicos` | Listar todos | 200 |
| GET | `/categorias/{id}` · `/tecnicos/{id}` | Buscar por ID | 200 |
| PUT | `/categorias/{id}` · `/tecnicos/{id}` | Atualizar | 200 |
| DELETE | `/categorias/{id}` · `/tecnicos/{id}` | Excluir | 204 |

O perfil do técnico aceita `ADMIN` ou `TECNICO`.

### Chamados

| Método | Rota | Ação | Sucesso |
|---|---|---|---|
| POST | `/chamados` | Abrir chamado (nasce `ABERTO`) | 201 |
| GET | `/chamados` | Listar com filtros e paginação | 200 |
| GET | `/chamados/{id}` | Detalhe do chamado com comentários | 200 |
| PUT | `/chamados/{id}` | Atualizar dados | 200 |
| PATCH | `/chamados/{id}/atribuir` | Atribuir técnico | 200 |
| PATCH | `/chamados/{id}/status` | Alterar status | 200 |
| DELETE | `/chamados/{id}` | Excluir | 204 |
| POST | `/chamados/{id}/comentarios` | Adicionar comentário | 201 |
| GET | `/chamados/{id}/comentarios` | Listar comentários | 200 |

Transições de status permitidas:

| De | Para |
|---|---|
| ABERTO | EM_ATENDIMENTO, FECHADO |
| EM_ATENDIMENTO | AGUARDANDO_CLIENTE, FECHADO |
| AGUARDANDO_CLIENTE | EM_ATENDIMENTO, FECHADO |
| FECHADO | nenhuma: chamado fechado não pode ser alterado |

Ao fechar, `dataFechamento` é preenchida automaticamente.

### Erros

| Status | Quando |
|---|---|
| 400 | Dados inválidos no corpo ou parâmetro de URL inválido |
| 404 | Recurso inexistente |
| 409 | Regra de negócio violada (duplicidade, transição de status inválida, chamado fechado, exclusão de registro com chamados) |

## Filtros, paginação e ordenação

`GET /chamados` aceita os filtros opcionais abaixo, que podem ser combinados:

| Parâmetro | Valores |
|---|---|
| `status` | `ABERTO`, `EM_ATENDIMENTO`, `AGUARDANDO_CLIENTE`, `FECHADO` |
| `prioridade` | `BAIXA`, `MEDIA`, `ALTA`, `CRITICA` |
| `clienteId` | ID do cliente |

As rotas `GET /chamados` e `GET /clientes/{id}/chamados` aceitam também:

| Parâmetro | Descrição | Padrão |
|---|---|---|
| `page` | Número da página, começando em 0 | `0` |
| `size` | Itens por página | `10` |
| `sort` | Campo e direção, ex.: `titulo,asc`. Pode repetir para ordenar por mais de um campo | `dataAbertura,desc` |

Campos de ordenação: `id`, `titulo`, `dataAbertura`, `dataFechamento`, `prioridade`, `status`,
`cliente.nome`, `categoria.nome` e `tecnico.nome`.

`prioridade` e `status` são ordenados em ordem alfabética, porque são gravados como texto.

### Exemplos de consulta

```bash
# Todos os chamados, mais recentes primeiro
curl -i "http://localhost:8080/chamados"

# Chamados abertos
curl -i "http://localhost:8080/chamados?status=ABERTO"

# Chamados de prioridade alta
curl -i "http://localhost:8080/chamados?prioridade=ALTA"

# Chamados em atendimento, de prioridade alta, do cliente 1
curl -i "http://localhost:8080/chamados?status=EM_ATENDIMENTO&prioridade=ALTA&clienteId=1"

# Segunda página, com 5 chamados por página
curl -i "http://localhost:8080/chamados?page=1&size=5"

# Ordenados pelo nome do cliente e depois pelo título
curl -i "http://localhost:8080/chamados?sort=cliente.nome,asc&sort=titulo,asc"

# Chamados do cliente 1
curl -i "http://localhost:8080/clientes/1/chamados"
```

Exemplo de resposta paginada:

```json
{
  "content": [
    {
      "id": 1,
      "titulo": "PC não liga",
      "descricao": "Computador não liga desde ontem",
      "dataAbertura": "2026-11-03T20:14:36.1",
      "dataFechamento": null,
      "prioridade": "ALTA",
      "status": "EM_ATENDIMENTO",
      "clienteId": 1,
      "clienteNome": "Ana",
      "categoriaId": 1,
      "categoriaNome": "Hardware",
      "tecnicoId": 1,
      "tecnicoNome": "Bruno"
    }
  ],
  "page": { "size": 10, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

Parâmetros inválidos retornam 400. Exemplos: `?status=XYZ`, `?clienteId=abc` ou `?sort=campoInexistente`.
