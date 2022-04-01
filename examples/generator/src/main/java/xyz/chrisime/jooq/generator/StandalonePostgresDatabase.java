package xyz.chrisime.jooq.generator;

import static java.lang.System.getProperty;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Function;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.meta.postgres.PostgresDatabase;
import org.jooq.tools.JooqLogger;
import org.jooq.tools.jdbc.JDBCUtils;
import org.postgresql.Driver;
import org.testcontainers.containers.PostgreSQLContainer;

public class StandalonePostgresDatabase extends PostgresDatabase {
    static {
        DEFAULT_FLYWAY_LOCATION = getProperty("flywayLocation", "filesystem:src/main/resources/db/migration");
        DEFAULT_DOCKER_IMAGE = "postgres:latest";
        ERR_MSG = (s) -> "Unable to start the database container and migrate the schemas: " + s;
        LOGGER = JooqLogger.getLogger(StandalonePostgresDatabase.class);
    }

    private static final JooqLogger LOGGER;

    private static final String DEFAULT_FLYWAY_LOCATION;

    private static final String DEFAULT_DOCKER_IMAGE;

    private static final Function<String, String> ERR_MSG;

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private PostgreSQLContainer<?> container;

    private Connection connection;

    @Override
    protected DSLContext create0() {
        if (connection == null) {
            try {
                createAndStartPostgresContainer();

                connectToJdbcDriver();

                runFlywayMigration();

                setConnection(connection);
            } catch (Exception exception) {
                LOGGER.error(ERR_MSG.apply(exception.getMessage()));
                throw new DataAccessException(ERR_MSG.apply(exception.getMessage()), exception);
            }
        }
        return DSL.using(connection, SQLDialect.POSTGRES);
    }

    private void createAndStartPostgresContainer() {
        LOGGER.info("Creating and starting the postgresql container...");

        container = new PostgreSQLContainer<>(DEFAULT_DOCKER_IMAGE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD);

        container.start();
    }

    private void connectToJdbcDriver() throws SQLException {
        LOGGER.info("Connecting to " + container.getJdbcUrl());

        Properties properties = new Properties();
        properties.put("user", USERNAME);
        properties.put("password", PASSWORD);

        connection = new Driver().connect(container.getJdbcUrl(), properties);
    }

    private void runFlywayMigration() {
        LOGGER.info("Executing flyway migration scripts...");

        Flyway flyway = Flyway.configure()
                              .dataSource(container.getJdbcUrl(), USERNAME, PASSWORD)
                              .locations(DEFAULT_FLYWAY_LOCATION)
                              .schemas("public")
                              .load();

        flyway.migrate();
    }

    @Override
    public void close() {
        LOGGER.info("Closing database connection...");
        JDBCUtils.safeClose(connection);
        connection = null;

        LOGGER.info("Stopping postgresql container...");
        container.stop();
    }
}
