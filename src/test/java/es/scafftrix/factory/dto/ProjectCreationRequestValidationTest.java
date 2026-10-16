package es.scafftrix.factory.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectCreationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void blankVersionReportsOnlyTheRequiredMessage() {
        ProjectCreationRequest request = new ProjectCreationRequest();
        request.setVersion("");

        Set<String> versionMessages = validator.validate(request).stream()
                .filter(v -> "version".equals(v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(versionMessages)
                .as("Una versión vacía no debe disparar además el mensaje de formato")
                .containsExactly("La versión del proyecto es obligatoria.");
    }

    @Test
    void validSnapshotVersionPasses() {
        ProjectCreationRequest request = new ProjectCreationRequest();
        request.setVersion("1.0.0-SNAPSHOT");

        assertThat(validator.validate(request).stream()
                .noneMatch(v -> "version".equals(v.getPropertyPath().toString()))).isTrue();
    }
}
