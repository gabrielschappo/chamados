package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;

public record ChamadoDetalheResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        Prioridade prioridade,
        StatusChamado status,
        Long clienteId,
        String clienteNome,
        Long categoriaId,
        String categoriaNome,
        Long tecnicoId,
        String tecnicoNome,
        List<ComentarioResponse> comentarios
) {
    public static ChamadoDetalheResponse de(Chamado c, List<Comentario> comentarios) {
        Tecnico t = c.getTecnico();
        return new ChamadoDetalheResponse(
                c.getId(), c.getTitulo(), c.getDescricao(),
                c.getDataAbertura(), c.getDataFechamento(),
                c.getPrioridade(), c.getStatus(),
                c.getCliente().getId(), c.getCliente().getNome(),
                c.getCategoria().getId(), c.getCategoria().getNome(),
                t != null ? t.getId() : null, t != null ? t.getNome() : null,
                comentarios.stream().map(ComentarioResponse::de).toList());
    }
}
