package xyz.chrisime.jooq.generator;

import static java.lang.System.getProperty;

import java.sql.Connection;
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

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final JooqLogger LOGGER = JooqLogger.getLogger(StandalonePostgresDatabase.class);

    private static final String DEFAULT_FLYWAY_LOCATION = getProperty("flywayLocation", "filesystem:src/main/resources/db/migration");

    private static final String DEFAULT_DOCKER_IMAGE= "postgres:latest";

    private static final Properties JDBC_PROPERTIES;

    private static final Function<String, String> ERR_MSG;

    private PostgreSQLContainer<?> container;

    private Connection connection;

    static {
        JDBC_PROPERTIES = new Properties();
        JDBC_PROPERTIES.put("user", USERNAME);
        JDBC_PROPERTIES.put("password", PASSWORD);

        ERR_MSG = (s) -> "Unable to start the database container and migrate the schemas: " + s;
    }

    @Override
    protected DSLContext create0() {
        if (connection == null) {
            try {
                LOGGER.info("Creating and starting the postgresql container...");
                container = new PostgreSQLContainer<>(DEFAULT_DOCKER_IMAGE)
                    .withUsername(USERNAME)
                    .withPassword(PASSWORD);
                container.start();

                LOGGER.info("Connecting to " + container.getJdbcUrl());
                connection = new Driver().connect(container.getJdbcUrl(), JDBC_PROPERTIES);

                LOGGER.info("Executing flyway migration scripts...");
                Flyway.configure()
                      .dataSource(container.getJdbcUrl(), USERNAME, PASSWORD)
                      .locations(DEFAULT_FLYWAY_LOCATION)
                      .schemas("public")
                      .load()
                      .migrate();

                setConnection(connection);
            } catch (Exception exception) {
                LOGGER.error(ERR_MSG.apply(exception.getMessage()));
                throw new DataAccessException(ERR_MSG.apply(exception.getMessage()), exception);
            }
        }
        return DSL.using(connection, SQLDialect.POSTGRES);
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
