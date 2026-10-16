package es.scafftrix.factory.dto;

import es.scafftrix.factory.domain.ProjectStatus;
import es.scafftrix.factory.domain.ProjectType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectSearchCriteriaTest {

    @Test
    void normalizedAppliesSafeDefaultsAndBounds() {
        ProjectSearchCriteria normalized = new ProjectSearchCriteria(
                "  customer  ",
                ProjectType.SPRING_BOOT,
                ProjectStatus.GENERATED,
                "createdAt;drop",
                -4,
                500)
                .normalized();

        assertEquals("customer", normalized.search());
        assertEquals(ProjectType.SPRING_BOOT, normalized.type());
        assertEquals(ProjectStatus.GENERATED, normalized.status());
        assertEquals(ProjectSearchCriteria.SORT_NEWEST, normalized.sort());
        assertEquals(0, normalized.page());
        assertEquals(100, normalized.size());
    }

    @Test
    void normalizedTreatsBlankSearchAndMissingSizeAsDefaults() {
        ProjectSearchCriteria normalized = new ProjectSearchCriteria("   ", null, null, null, 0, 0)
                .normalized();

        assertNull(normalized.search());
        assertEquals(ProjectSearchCriteria.SORT_NEWEST, normalized.sort());
        assertEquals(20, normalized.size());
    }

    @Test
    void normalizedTreatsMissingPageAndSizeAsDefaults() {
        ProjectSearchCriteria normalized = new ProjectSearchCriteria(null, null, null, null, null, null)
                .normalized();

        assertEquals(ProjectSearchCriteria.DEFAULT_PAGE, normalized.page());
        assertEquals(ProjectSearchCriteria.DEFAULT_SIZE, normalized.size());
    }
}
