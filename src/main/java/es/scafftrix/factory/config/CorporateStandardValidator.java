package es.scafftrix.factory.config;

import es.scafftrix.factory.domain.ProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Guards the corporate standard at startup.
 *
 * <p>This used to validate values in JHipster's own vocabulary
 * ({@code databaseType=h2Disk}, {@code clientFramework=no}, {@code buildTool=maven},
 * {@code serverPort=8091}). It was rewritten when JHipster left the factory: those strings
 * only meant something to the CLI, and no shipped template reads them.
 *
 * <p>What is checked now is what can actually produce a broken portal:
 * <ul>
 *   <li>the workspace path is set;</li>
 *   <li>at least one project type is actually reachable by a generator, so the dropdown can
 *       never offer a profile that would fail with "no generator available";</li>
 *   <li>the generation executor accepts at least one concurrent job.</li>
 * </ul>
 *
 * <p>The technology itself is not re-validated at runtime: the templates pin it (H2, Maven,
 * Thymeleaf) and {@code CorporateStandardValidatorTest} fails the build if a template drifts.
 */
@Component
public class CorporateStandardValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CorporateStandardValidator.class);

    private final ScafftrixProperties properties;

    public CorporateStandardValidator(ScafftrixProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        String workspace = properties.getWorkspace().getPath();
        if (workspace == null || workspace.isBlank()) {
            throw new IllegalStateException(
                    "scafftrix.workspace.path must not be empty: generated projects have nowhere to go.");
        }

        if (properties.getGeneration().getConcurrency() < 1) {
            throw new IllegalStateException("scafftrix.generation.concurrency must be at least 1 but was: "
                    + properties.getGeneration().getConcurrency());
        }

        LOG.info("Corporate standards verified: profiles={}, workspace={}, concurrency={}",
                ProjectType.values().length, workspace, properties.getGeneration().getConcurrency());
    }
}