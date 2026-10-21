package br.com.sistemas.chamados.exception;

/** Lançada quando o recurso procurado não existe (vira HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
