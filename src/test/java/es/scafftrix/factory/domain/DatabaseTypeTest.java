package es.scafftrix.factory.domain;

import es.scafftrix.factory.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseTypeTest {

    @Test
    void resolvesByEnumNameDisplayNameAndJhipsterValueIgnoringCase() {
        assertThat(DatabaseType.fromStoredValue("H2")).isEqualTo(DatabaseType.H2);
        assertThat(DatabaseType.fromStoredValue("h2")).isEqualTo(DatabaseType.H2);
        assertThat(DatabaseType.fromStoredValue("h2Disk")).isEqualTo(DatabaseType.H2);
        assertThat(DatabaseType.fromStoredValue("POSTGRESQL")).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(DatabaseType.fromStoredValue("PostgreSQL")).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(DatabaseType.fromStoredValue("postgresql")).isEqualTo(DatabaseType.POSTGRESQL);
    }

    @Test
    void rejectsNullBlankAndUnsupportedValues() {
        assertThatThrownBy(() -> DatabaseType.fromStoredValue(null))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> DatabaseType.fromStoredValue("  "))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> DatabaseType.fromStoredValue("oracle"))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void exposesItsDisplayAndJhipsterValues() {
        assertThat(DatabaseType.H2.getDisplayName()).isEqualTo("H2");
        assertThat(DatabaseType.H2.getJhipsterValue()).isEqualTo("h2Disk");
        assertThat(DatabaseType.POSTGRESQL.getDisplayName()).isEqualTo("PostgreSQL");
        assertThat(DatabaseType.POSTGRESQL.getJhipsterValue()).isEqualTo("postgresql");
    }
}
