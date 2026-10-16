package es.scafftrix.factory.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectUpdateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messagesFor(String field, ProjectUpdateRequest request) {
        return validator.validate(request).stream()
                .filter(v -> field.equals(v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void blankNameIsRejected() {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("   ");
        request.setVersion("1.0.0");

        assertThat(messagesFor("name", request)).containsExactly("El nombre del proyecto es obligatorio.");
    }

    @Test
    void blankVersionReportsOnlyTheRequiredMessage() {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("Billing");
        request.setVersion("");

        assertThat(messagesFor("version", request))
                .containsExactly("La versión del proyecto es obligatoria.");
    }

    @Test
    void invalidVersionIsRejected() {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("Billing");
        request.setVersion("1.0.0; rm -rf /");

        assertThat(messagesFor("version", request)).isNotEmpty();
    }

    @Test
    void validRequestHasNoViolations() {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("Billing");
        request.setVersion("2.1.0-SNAPSHOT");
        request.setDescription("descripción");

        assertThat(validator.validate(request)).isEmpty();
    }
}
