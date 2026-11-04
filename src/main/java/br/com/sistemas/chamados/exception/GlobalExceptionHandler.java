package br.com.sistemas.chamados.exception;

import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.query.sqm.UnknownPathException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation. RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado (RecursoNaoEncontradoException ex) {
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity< ErroResposta> regraNegocio(RegraNegocioException ex) {
        return montar(HttpStatus. CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ResponseEntity<ErroResposta> transicaoInvalida(TransicaoStatusInvalidaException ex) {
        return montar(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhes);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                List.of(ex.getName() + ": valor '" + ex.getValue() + "' não é válido"));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErroResposta> ordenacaoInvalida(InvalidDataAccessApiUsageException ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof UnknownPathException || causa instanceof PathElementException) {
                return montar(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                        List.of("sort: campo de ordenação inexistente"));
            }
        }
        throw ex;
    }

    private ResponseEntity<ErroResposta > montar(
            HttpStatus status, String erro, List<String> detalhes) {
        return ResponseEntity.status(status)
                .body(new ErroResposta(LocalDateTime.now(), status. value(), erro, detalhes));
    }
}
