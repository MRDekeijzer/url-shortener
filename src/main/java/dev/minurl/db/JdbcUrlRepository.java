package dev.minurl.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

public class JdbcUrlRepository implements UrlRepository {
    private static final String SELECT_URL_BY_CODE = """
            SELECT normalized_url
            FROM short_urls
            WHERE code = ?
            """;
    private static final String SELECT_CODE_BY_URL = """
            SELECT code
            FROM short_urls
            WHERE normalized_url = ?
            """;
    private static final String INSERT_SQL = """
            INSERT INTO short_urls (code, normalized_url)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

    private final DataSource dataSource;

    public JdbcUrlRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<String> findNormalizedUrlByCode(String code) {
        return querySingleValue(SELECT_URL_BY_CODE, code);
    }

    @Override
    public Optional<String> findCodeByNormalizedUrl(String normalizedUrl) {
        return querySingleValue(SELECT_CODE_BY_URL, normalizedUrl);
    }

    @Override
    public boolean insert(String code, String normalizedUrl) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, code);
            statement.setString(2, normalizedUrl);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to insert short URL", ex);
        }
    }

    private Optional<String> querySingleValue(String sql, String parameter) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString(1));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Database query failed", ex);
        }
    }
}
