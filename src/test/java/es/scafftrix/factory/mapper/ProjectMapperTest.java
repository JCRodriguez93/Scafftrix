package es.scafftrix.factory.mapper;

import es.scafftrix.factory.domain.ConfigFormat;
import es.scafftrix.factory.domain.Project;
import es.scafftrix.factory.domain.ProjectCoordinates;
import es.scafftrix.factory.domain.ProjectType;
import es.scafftrix.factory.domain.TechnologyStack;
import es.scafftrix.factory.dto.ProjectDetailDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProjectMapperTest {

    private final ProjectMapper mapper = new ProjectMapper();

    @Test
    void historicProjectWithoutSelectorsMapsWithoutFailing() {
        Project legacy = new Project(
                "Legacy",
                new ProjectCoordinates("legacy", "es.scafftrix", "legacy", "es.scafftrix.legacy", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_BOOT,
                new TechnologyStack("H2", "Thymeleaf", "Maven"), null, null);

        assertThatCode(() -> mapper.toDetail(legacy)).doesNotThrowAnyException();

        ProjectDetailDto detail = mapper.toDetail(legacy);
        assertThat(detail.springBootVersion()).isNull();
        assertThat(detail.configFormat()).isNull();
        assertThat(detail.name()).isEqualTo("Legacy");
    }

    @Test
    void projectWithSelectorsKeepsTheChosenVersionAndFormat() {
        Project modern = new Project(
                "Modern",
                new ProjectCoordinates("modern", "es.scafftrix", "modern", "es.scafftrix.modern", "1.0.0-SNAPSHOT"),
                null, ProjectType.SPRING_MICROSERVICE,
                new TechnologyStack("H2", null, "Maven"), "4.1.1", ConfigFormat.YAML);

        ProjectDetailDto detail = mapper.toDetail(modern);
        assertThat(detail.springBootVersion()).isEqualTo("4.1.1");
        assertThat(detail.configFormat()).isEqualTo(ConfigFormat.YAML);
    }
}
