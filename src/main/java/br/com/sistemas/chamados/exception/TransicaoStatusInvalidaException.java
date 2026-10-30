package br.com.sistemas.chamados.exception;

/** Lançada quando a mudança de status do chamado não é permitida (vira HTTP 409). */
public class TransicaoStatusInvalidaException extends RuntimeException {
    public TransicaoStatusInvalidaException(String message) {
        super(message);
    }
}
