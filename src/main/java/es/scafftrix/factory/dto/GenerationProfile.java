package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.ProjectType;

/**
 * Immutable, fully validated description of the project to generate.
 *
 * <p>It carries only what the shipped templates substitute as {@code @token@} values. The
 * former JHipster-specific fields (database, frontend, buildTool, applicationType,
 * authenticationType, serverPort, enableTranslation, nativeLanguage, blueprints) were removed
 * because nothing read them: they only ever fed the {@code .yo-rc.json} of the JHipster CLI,
 * which no profile uses. The technology shown to the user now comes from the
 * {@link ProjectType} manifest of the selected profile.
 */
public record GenerationProfile(ProjectType projectType,
                                String applicationName,
                                String baseName,
                                String groupId,
                                String artifactId,
                                String packageName,
                                String version,
                                String springBootVersion,
                                ConfigFormat configFormat,
                                String description) {

    public String mavenCoordinates() {
        return groupId + ':' + artifactId + ':' + version;
    }
}
