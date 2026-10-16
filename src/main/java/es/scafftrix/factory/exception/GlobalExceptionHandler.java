package es.scafftrix.factory.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Central error handling.
 *
 * <p>{@link ScafftrixException} and its subclasses produce a safe, Spanish
 * user message and a technical message written to the log. Other runtime exceptions are
 * converted to a generic 500 and logged, so no stack trace leaks to the browser.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MODEL_TITLE = "title";
    private static final String MODEL_MESSAGE = "message";
    private static final String MODEL_TECHNICAL = "technical";

    @ExceptionHandler(ScafftrixException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleFactoryException(ScafftrixException ex, Model model) {
        model.addAttribute(MODEL_TITLE, "Error en la solicitud");
        model.addAttribute(MODEL_MESSAGE, ex.getUserMessage());
        model.addAttribute(MODEL_TECHNICAL, ex.getTechnicalMessage());
        return "error/400";
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ProjectNotFoundException ex, Model model) {
        model.addAttribute(MODEL_TITLE, "Proyecto no encontrado");
        model.addAttribute(MODEL_MESSAGE, ex.getUserMessage());
        model.addAttribute(MODEL_TECHNICAL, ex.getTechnicalMessage());
        return "error/404";
    }

    @ExceptionHandler({GenerationInProgressException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflict(ScafftrixException ex, Model model) {
        model.addAttribute(MODEL_TITLE, "Solicitud no permitida");
        model.addAttribute(MODEL_MESSAGE, ex.getUserMessage());
        model.addAttribute(MODEL_TECHNICAL, ex.getTechnicalMessage());
        return "error/409";
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(RuntimeException ex, Model model) {
        LOG.error("Unexpected unhandled exception", ex);
        model.addAttribute(MODEL_TITLE, "Error inesperado");
        model.addAttribute(MODEL_MESSAGE, "Se ha producido un error inesperado. Inténtelo de nuevo más tarde.");
        model.addAttribute(MODEL_TECHNICAL, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return "error/500";
    }
}