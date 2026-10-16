package es.scafftrix.factory.generator;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GenerationResultTest {

    @Test
    void successCarriesTheProjectRootAndNoError() {
        Path root = Path.of("work/project");

        GenerationResult result = GenerationResult.success("Plantilla", Duration.ofSeconds(3), root);

        assertThat(result.generator()).isEqualTo("Plantilla");
        assertThat(result.success()).isTrue();
        assertThat(result.duration()).isEqualTo(Duration.ofSeconds(3));
        assertThat(result.projectRoot()).isEqualTo(root);
        assertThat(result.errorSummary()).isNull();
    }

    @Test
    void failureCarriesTheSummaryAndNoProjectRoot() {
        GenerationResult result = GenerationResult.failure("Plantilla", Duration.ofSeconds(1), "boom");

        assertThat(result.success()).isFalse();
        assertThat(result.projectRoot()).isNull();
        assertThat(result.errorSummary()).isEqualTo("boom");
    }

    @Test
    void progressListenerNoopAcceptsEveryStep() {
        assertThatCode(() -> ProjectGenerator.ProgressListener.NOOP.onStep("cualquier paso"))
                .doesNotThrowAnyException();
    }
}
