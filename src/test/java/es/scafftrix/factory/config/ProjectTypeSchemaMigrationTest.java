package es.scafftrix.factory.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectTypeSchemaMigrationTest {

    private final ProjectTypeSchemaMigration migration = new ProjectTypeSchemaMigration();

    @Test
    void convertsAnExistingEnumColumnKeepingStoredRowsAndAcceptingNewValues() throws Exception {
        DataSource dataSource = inMemory("legacyEnum");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE project (id int primary key, "
                    + "project_type ENUM('SPRING_BATCH', 'SPRING_BOOT') not null)");
            statement.execute("INSERT INTO project (id, project_type) VALUES (1, 'SPRING_BOOT')");

            migration.normalize(dataSource);

            statement.execute("INSERT INTO project (id, project_type) VALUES (2, 'SPRING_MICROSERVICE')");
            try (ResultSet rs = statement.executeQuery("SELECT project_type FROM project ORDER BY id")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("SPRING_BOOT");
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("SPRING_MICROSERVICE");
            }
        }
    }

    @Test
    void isIdempotentOnAnAlreadyVarcharColumn() throws Exception {
        DataSource dataSource = inMemory("freshVarchar");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE project (id int primary key, project_type varchar(40))");

            migration.normalize(dataSource);
            migration.normalize(dataSource);

            statement.execute("INSERT INTO project (id, project_type) VALUES (1, 'SPRING_MICROSERVICE')");
            try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM project")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void doesNothingWhenTheTableDoesNotExistYet() throws Exception {
        DataSource dataSource = inMemory("emptyDatabase");

        migration.normalize(dataSource);

        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, "PROJECT", null)) {
            assertThat(tables.next()).isFalse();
        }
    }

    @Test
    void doesNothingOnDatabasesThatAlreadyStoreAPlainType() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");

        migration.normalize(dataSource);

        verify(connection, never()).createStatement();
    }

    @Test
    void refusesToStartWhenTheDatabaseCannotBeReached() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        assertThatThrownBy(() -> migration.normalize(dataSource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("project_type");
    }

    private static DataSource inMemory(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
