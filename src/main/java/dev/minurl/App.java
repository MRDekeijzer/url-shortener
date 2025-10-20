package dev.minurl;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import dev.minurl.db.DataSourceFactory;
import dev.minurl.db.DatabaseMigrator;
import dev.minurl.db.JdbcUrlRepository;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;

public class App {
    private static final Logger REQUEST_LOGGER = LoggerFactory.getLogger("dev.minurl.requests");
    private static final String AUTH_USER_ATTRIBUTE = "authenticatedUser";

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        Map<String, String> hashedUsers = config.basicAuthUsers();
        if (hashedUsers.isEmpty()) {
            throw new IllegalStateException("BASIC_AUTH_USERS must define at least one user");
        }
        var dataSource = DataSourceFactory.create(config);
        var migrator = new DatabaseMigrator(
                dataSource,
                "liquibase/changelog-master.yaml",
                "local".equals(config.environment()) ? "local" : null);
        migrator.migrate();

        UrlShortenerService service = new UrlShortenerService(
                new JdbcUrlRepository(dataSource),
                new UrlNormalizer(),
                new DeterministicCodeGenerator(),
                config.urlMinLength());

        String baseUrl = config.baseUrl();

        var app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                if ("local".equals(config.environment())) {
                    rule.anyHost();
                } else {
                    rule.allowHost(config.baseUrl());
                }
            }));
        });

        app.before(ctx -> ctx.attribute("request-start-time", System.nanoTime()));

        app.post("/api/shorten", ctx -> {
            requireAuth(ctx, hashedUsers);
            var body = ctx.bodyAsClass(ShortenReq.class);
            if (body == null || body.url == null)
                throw new BadRequestResponse("missing url");
            String code = service.shorten(body.url);
            ctx.json(new ShortenRes(baseUrl + "/" + code));
        });

        app.get("/{code}", ctx -> {
            String code = ctx.pathParam("code");
            ctx.redirect(service.resolve(code));
        });

        app.get("/api/health", ctx -> ctx.json(Map.of("status", "ok")));

        app.after(ctx -> {
            Long start = ctx.attribute("request-start-time");
            long durationMs = start == null ? -1L : (System.nanoTime() - start) / 1_000_000;
            int status = ctx.res().getStatus();
            REQUEST_LOGGER.info("{} {} {} {}ms",
                    ctx.method(),
                    ctx.fullUrl(),
                    status,
                    durationMs);
        });

        app.start(config.port());
    }

    private static void requireAuth(Context ctx, Map<String, String> hashedUsers) {
        var credentials = ctx.basicAuthCredentials();
        if (credentials == null) {
            throw unauthorized(ctx);
        }

        String storedHash = hashedUsers.get(credentials.getUsername());
        if (storedHash == null || !BCrypt.checkpw(credentials.getPassword(), storedHash)) {
            throw unauthorized(ctx);
        }

        ctx.attribute(AUTH_USER_ATTRIBUTE, credentials.getUsername());
    }

    private static UnauthorizedResponse unauthorized(Context ctx) {
        ctx.header("WWW-Authenticate", "Basic realm=\"minurl\"");
        return new UnauthorizedResponse("Missing or invalid credentials");
    }

    public static class ShortenReq {
        public String url;

        public ShortenReq() {
        }
    }

    public static class ShortenRes {
        public String shortUrl;

        public ShortenRes(String s) {
            this.shortUrl = s;
        }
    }
}
