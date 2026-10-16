package es.scafftrix.factory.exception;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScafftrixExceptionTest {

    @Test
    void everyDeliberateExceptionCarriesBothMessages() {
        assertThat(new CompressionException("boom", new IOException()).getUserMessage()).isNotBlank();

        assertThat(new WorkspaceUnavailableException("boom", null).getTechnicalMessage())
                .contains("Workspace unavailable");

        assertThat(new GenerationTimeoutException(Duration.ofMinutes(1), "detail").getTechnicalMessage())
                .contains("timed out");

        assertThat(new DownloadException("user", "tech").getUserMessage()).isEqualTo("user");
        assertThat(new DownloadException("user", "tech", new IOException()).getCause()).isNotNull();

        assertThat(new InvalidConfigurationException("user", "tech").getTechnicalMessage()).isEqualTo("tech");
        assertThat(new InvalidConfigurationException("user", "tech", new IOException()).getCause()).isNotNull();

        UUID id = UUID.randomUUID();
        assertThat(new GenerationInProgressException(id).getTechnicalMessage()).contains(id.toString());
        assertThat(new ProjectNotFoundException(id).getTechnicalMessage()).contains(id.toString());
    }

    @Test
    void userAndTechnicalMessagesAreIndependent() {
        ScafftrixException exception = new DownloadException("mensaje visible", "detalle interno");

        assertThat(exception.getUserMessage()).isEqualTo("mensaje visible");
        assertThat(exception.getTechnicalMessage()).isEqualTo("detalle interno");
    }
}
