package es.scafftrix.factory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Makes {@code project.project_type} a plain {@code varchar} instead of a native H2 {@code ENUM}.
 *
 * <p><strong>Why this exists.</strong> On H2, Hibernate 6 maps
 * {@code @Enumerated(EnumType.STRING)} to the database's native {@code ENUM} type. The allowed
 * values become part of the column definition the first time the table is created:
 *
 * <pre>project_type ENUM('SPRING_BATCH', 'SPRING_BOOT')</pre>
 *
 * {@code spring.jpa.hibernate.ddl-auto=update} adds columns and widens plain columns, but it
 * does <strong>not</strong> widen an existing {@code ENUM}. Adding a constant to
 * {@code ProjectType} was therefore never enough on its own for databases created before that
 * constant existed: every project of the new type was rejected at insert time with
 * {@code DataIntegrityViolationException: Value not permitted for column ...}.
 *
 * <p><strong>How it is fixed.</strong> The column is normalised to a plain {@code varchar}.
 * A {@code varchar} accepts any enum name, so adding a {@link
 * es.scafftrix.factory.domain.ProjectType} never needs a schema migration again, and the
 * column can be read back without relying on H2 internals.
 *
 * <p>The statement is a single constant: no value is ever interpolated, so it carries no SQL
 * injection surface. It is also idempotent and additive: on an old
 * database it converts the existing {@code ENUM} preserving every stored row, and on a database
 * created from scratch (where the entity already declares a {@code varchar}) it simply re-sets
 * the same type and changes nothing. It is safe to run on every startup.
 *
 * <p>Databases other than H2 already store the column as a plain type and need no migration, so
 * the runner does nothing on them.
 */
@Configuration
public class ProjectTypeSchemaMigration {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTypeSchemaMigration.class);

    private static final String TABLE = "PROJECT";
    private static final String COLUMN = "PROJECT_TYPE";

    /**
     * Constant, idempotent DDL: converts an old native {@code ENUM} column to a plain
     * {@code varchar(40)} and leaves an already-{@code varchar} one untouched. Because nothing
     * is concatenated into it, the statement cannot be abused to inject SQL.
     */
    private static final String SET_VARCHAR =
            "ALTER TABLE " + TABLE + " ALTER COLUMN " + COLUMN + " SET DATA TYPE VARCHAR(40)";

    @Bean
    public CommandLineRunner normalizeProjectTypeColumn(DataSource dataSource) {
        return args -> normalize(dataSource);
    }

    void normalize(DataSource dataSource) {
        String product;
        try (Connection connection = dataSource.getConnection()) {
            product = connection.getMetaData().getDatabaseProductName();
            if (!isH2(product)) {
                LOG.info("No project_type migration needed: {} stores the column as a plain type", product);
                return;
            }
            if (!tableExists(connection)) {
                LOG.info("No project_type migration needed: table {} does not exist yet", TABLE);
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(SET_VARCHAR);
            }
            LOG.info("project_type stores every ProjectType as a plain varchar");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not normalise the project_type column in the database. Without it the factory cannot "
                            + "guarantee that every ProjectType is storable, so it refuses to start rather than "
                            + "failing later, on the first project creation.", e);
        }
    }

    private boolean isH2(String databaseProductName) {
        return databaseProductName != null
                && databaseProductName.toLowerCase(Locale.ROOT).contains("h2");
    }

    private boolean tableExists(Connection connection) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, TABLE, null)) {
            return tables.next();
        }
    }
}
