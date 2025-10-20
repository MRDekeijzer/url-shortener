package dev.minurl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Centralised application configuration backed by environment variables with
 * typed validation.
 */
public final class AppConfig {
    public static final String DEFAULT_BASE_URL = "http://localhost:7000";
    public static final int DEFAULT_PORT = 7000;
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 60;
    public static final int DEFAULT_URL_MIN_LENGTH = 1;
    public static final String DEFAULT_ENVIRONMENT = "local";
    public static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/minurl";
    public static final String DEFAULT_DB_USER = "minurl";
    public static final String DEFAULT_DB_PASSWORD = "minurl";

    private final String baseUrl;
    private final int port;
    private final int urlMinLength;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int rateLimitPerMinute;
    private final String environment;
    private final Map<String, String> basicAuthUsers;

    private AppConfig(String baseUrl,
            int port,
            int urlMinLength,
            String dbUrl,
            String dbUser,
            String dbPassword,
            int rateLimitPerMinute,
            String environment,
            Map<String, String> basicAuthUsers) {
        this.baseUrl = baseUrl;
        this.port = port;
        this.urlMinLength = urlMinLength;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.environment = environment;
        this.basicAuthUsers = basicAuthUsers;
    }

    public static AppConfig load() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String baseUrl = stripTrailingSlash(readOrDefault(dotenv, "BASE_URL", DEFAULT_BASE_URL));
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("BASE_URL must not be blank");
        }

        String environment = readOrDefault(dotenv, "APP_ENV", DEFAULT_ENVIRONMENT).toLowerCase();
        if (!environment.matches("local|prod")) {
            throw new IllegalArgumentException("APP_ENV must be either 'local' or 'prod'");
        }

        int port = parsePositiveInt(readOrDefault(dotenv, "PORT", Integer.toString(DEFAULT_PORT)), "PORT");
        int urlMinLength = parsePositiveInt(
                readOrDefault(dotenv, "URL_MIN_LENGTH", Integer.toString(DEFAULT_URL_MIN_LENGTH)),
                "URL_MIN_LENGTH");
        if (urlMinLength > 42) {
            throw new IllegalArgumentException("URL_MIN_LENGTH must be between 1 and 42");
        }
        int rateLimit = parsePositiveInt(
                readOrDefault(dotenv, "RATE_LIMIT_PER_MINUTE", Integer.toString(DEFAULT_RATE_LIMIT_PER_MINUTE)),
                "RATE_LIMIT_PER_MINUTE");

        String dbUrl = readOrDefault(dotenv, "DB_URL", DEFAULT_DB_URL);
        String dbUser = readOrDefault(dotenv, "DB_USER", DEFAULT_DB_USER);
        String dbPassword = readOrDefault(dotenv, "DB_PASSWORD", DEFAULT_DB_PASSWORD);

        if (dbUrl.isBlank()) {
            throw new IllegalArgumentException("DB_URL must not be blank");
        }

        if (dbUser.isBlank()) {
            throw new IllegalArgumentException("DB_USER must not be blank");
        }

        if (dbPassword.isBlank()) {
            throw new IllegalArgumentException("DB_PASSWORD must not be blank");
        }

        Map<String, String> basicAuthUsers = parseBasicAuthUsers(readOptional(dotenv, "BASIC_AUTH_USERS"));

        return new AppConfig(baseUrl,
                port,
                urlMinLength,
                dbUrl,
                dbUser,
                dbPassword,
                rateLimit,
                environment,
                basicAuthUsers);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public int port() {
        return port;
    }

    public int urlMinLength() {
        return urlMinLength;
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

    public String environment() {
        return environment;
    }

    public Map<String, String> basicAuthUsers() {
        return basicAuthUsers;
    }

    private static String readOrDefault(Dotenv dotenv, String key, String defaultValue) {
        String value = readOptional(dotenv, key);
        return value != null ? value : defaultValue;
    }

    private static String stripTrailingSlash(String value) {
        int schemeIndex = value.indexOf("://");
        int cutoff = schemeIndex >= 0 ? schemeIndex + 3 : 0;
        int end = value.length();
        while (end > cutoff && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
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

    private static Map<String, String> parseBasicAuthUsers(String rawUsers) {
        if (rawUsers == null || rawUsers.isBlank()) {
            return Map.of();
        }

        String[] entries = rawUsers.split(",");
        Map<String, String> result = new LinkedHashMap<>();
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "BASIC_AUTH_USERS entries must be in the form username:bcryptHash");
            }
            String username = parts[0].trim();
            String bcryptHash = parts[1].trim();
            if (username.isEmpty()) {
                throw new IllegalArgumentException("BASIC_AUTH_USERS usernames must not be blank");
            }
            if (bcryptHash.isEmpty()) {
                throw new IllegalArgumentException("BASIC_AUTH_USERS hashes must not be blank");
            }
            if (!bcryptHash.startsWith("$2")) {
                throw new IllegalArgumentException(
                        "BASIC_AUTH_USERS hashes must be BCrypt (prefix $2)");
            }
            if (result.put(username, bcryptHash) != null) {
                throw new IllegalArgumentException("Duplicate username in BASIC_AUTH_USERS: " + username);
            }
        }

        return Collections.unmodifiableMap(result);
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
