package dev.minurl;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Centralised application configuration backed by environment variables with
 * typed validation.
 */
public final class AppConfig {
    public static final String DEFAULT_BASE_URL = "http://localhost:7000";
    public static final int DEFAULT_PORT = 7000;
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 60;

    private final String baseUrl;
    private final int port;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int rateLimitPerMinute;

    private AppConfig(String baseUrl,
            int port,
            String dbUrl,
            String dbUser,
            String dbPassword,
            int rateLimitPerMinute) {
        this.baseUrl = baseUrl;
        this.port = port;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public static AppConfig load() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String baseUrl = readOrDefault(dotenv, "BASE_URL", DEFAULT_BASE_URL);
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("BASE_URL must not be blank");
        }

        int port = parsePositiveInt(readOrDefault(dotenv, "PORT", Integer.toString(DEFAULT_PORT)), "PORT");
        int rateLimit = parsePositiveInt(
                readOrDefault(dotenv, "RATE_LIMIT_PER_MINUTE", Integer.toString(DEFAULT_RATE_LIMIT_PER_MINUTE)),
                "RATE_LIMIT_PER_MINUTE");

        String dbUrl = readOptional(dotenv, "DB_URL");
        String dbUser = readOptional(dotenv, "DB_USER");
        String dbPassword = readOptional(dotenv, "DB_PASSWORD");

        if ((dbUser != null || dbPassword != null) && dbUrl == null) {
            throw new IllegalArgumentException("DB_URL is required when DB_USER or DB_PASSWORD is provided");
        }

        return new AppConfig(baseUrl, port, dbUrl, dbUser, dbPassword, rateLimit);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public int port() {
        return port;
    }

    public String dbUrl() {
        return dbUrl;
    }

    public String dbUser() {
        return dbUser;
    }

    public String dbPassword() {
        return dbPassword;
    }

    public int rateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    private static String readOrDefault(Dotenv dotenv, String key, String defaultValue) {
        String value = readOptional(dotenv, key);
        return value != null ? value : defaultValue;
    }

    private static String readOptional(Dotenv dotenv, String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromDotEnv = dotenv.get(key);
        if (fromDotEnv != null && !fromDotEnv.isBlank()) {
            return fromDotEnv.trim();
        }
        return null;
    }

    private static int parsePositiveInt(String rawValue, String key) {
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(key + " must be a positive integer");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a valid integer", ex);
        }
    }
}
