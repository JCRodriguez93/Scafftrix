package @packageName@.api.exception;

import @packageName@.domain.exception.CenterNotFoundException;
import @packageName@.domain.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio a respuestas HTTP.
 *
 * <p>Es la unica capa que conoce {@code ProblemDetail} (RFC 7807): asi el dominio y la
 * aplicacion no se acoplan al protocolo HTTP.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({UserNotFoundException.class, CenterNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Recurso no encontrado");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Peticion invalida");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Peticion no valida");
        return problem;
    }
}
