package dev.minurl.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public final class DatabaseMigrator {
    private final DataSource dataSource;
    private final String changeLogPath;
    private final Contexts contexts;

    public DatabaseMigrator(DataSource dataSource, String changeLogPath, String contexts) {
        this.dataSource = dataSource;
        this.changeLogPath = changeLogPath;
        this.contexts = (contexts == null || contexts.isBlank()) ? new Contexts() : new Contexts(contexts);
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(changeLogPath,
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.update(contexts, new LabelExpression());
            }
        } catch (SQLException | LiquibaseException ex) {
            throw new IllegalStateException("Failed to apply database migrations", ex);
        }
    }
}
