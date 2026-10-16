package es.scafftrix.factory.exception;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void factoryExceptionsRenderTheBadRequestPage() {
        Model model = new ExtendedModelMap();
        InvalidConfigurationException ex = new InvalidConfigurationException("visible", "técnico");

        String view = handler.handleFactoryException(ex, model);

        assertThat(view).isEqualTo("error/400");
        assertThat(model.getAttribute("title")).isEqualTo("Error en la solicitud");
        assertThat(model.getAttribute("message")).isEqualTo("visible");
        assertThat(model.getAttribute("technical")).isEqualTo("técnico");
    }

    @Test
    void missingProjectsRenderTheNotFoundPage() {
        Model model = new ExtendedModelMap();
        ProjectNotFoundException ex = new ProjectNotFoundException(UUID.randomUUID());

        String view = handler.handleNotFound(ex, model);

        assertThat(view).isEqualTo("error/404");
        assertThat(model.getAttribute("title")).isEqualTo("Proyecto no encontrado");
        assertThat(model.getAttribute("message")).isEqualTo(ex.getUserMessage());
    }

    @Test
    void generationConflictsRenderTheConflictPage() {
        Model model = new ExtendedModelMap();
        GenerationInProgressException ex = new GenerationInProgressException(UUID.randomUUID());

        String view = handler.handleConflict(ex, model);

        assertThat(view).isEqualTo("error/409");
        assertThat(model.getAttribute("title")).isEqualTo("Solicitud no permitida");
    }

    @Test
    void unexpectedExceptionsRenderTheGenericServerErrorPage() {
        Model model = new ExtendedModelMap();

        String view = handler.handleUnexpected(new IllegalStateException("kaboom"), model);

        assertThat(view).isEqualTo("error/500");
        assertThat(model.getAttribute("title")).isEqualTo("Error inesperado");
        assertThat(model.getAttribute("message").toString()).contains("error inesperado");
        assertThat(model.getAttribute("technical").toString()).contains("IllegalStateException");
    }
}
