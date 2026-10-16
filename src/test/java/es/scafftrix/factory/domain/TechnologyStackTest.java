package es.scafftrix.factory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TechnologyStackTest {

    @Test
    void exposesThePinnedValues() {
        TechnologyStack stack = new TechnologyStack("H2", "Thymeleaf", "Maven");

        assertThat(stack.database()).isEqualTo("H2");
        assertThat(stack.frontend()).isEqualTo("Thymeleaf");
        assertThat(stack.buildTool()).isEqualTo("Maven");
    }

    @Test
    void nullFrontendBecomesEmpty() {
        TechnologyStack stack = new TechnologyStack("H2", null, "Maven");

        assertThat(stack.frontend()).isEmpty();
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThatThrownBy(() -> new TechnologyStack(" ", "Thymeleaf", "Maven"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TechnologyStack("H2", "Thymeleaf", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
