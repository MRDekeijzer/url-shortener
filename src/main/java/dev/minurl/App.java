package dev.minurl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.minurl.db.DataSourceFactory;
import dev.minurl.db.DatabaseMigrator;
import dev.minurl.db.JdbcUrlRepository;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;

public class App {
    private static final Logger REQUEST_LOGGER = LoggerFactory.getLogger("dev.minurl.requests");

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
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

        app.get("/healthz", ctx -> ctx.result("ok"));

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
