package br.com.sistemas.chamados.entity;

import java.util.Set;

public enum StatusChamado {
    ABERTO,
    EM_ATENDIMENTO,
    AGUARDANDO_CLIENTE,
    FECHADO;

    /** Transições permitidas a partir de cada status. FECHADO é final. */
    public boolean podeMudarPara(StatusChamado novo) {
        return switch (this) {
            case ABERTO -> Set.of(EM_ATENDIMENTO, FECHADO).contains(novo);
            case EM_ATENDIMENTO -> Set.of(AGUARDANDO_CLIENTE, FECHADO).contains(novo);
            case AGUARDANDO_CLIENTE -> Set.of(EM_ATENDIMENTO, FECHADO).contains(novo);
            case FECHADO -> false;
        };
    }
}
