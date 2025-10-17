package dev.minurl;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class App {
    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        String baseUrl = config.baseUrl();
        UrlStore store = new UrlStore();

        var app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(r -> r.anyHost()));
        });

        app.post("/api/shorten", ctx -> {
            var body = ctx.bodyAsClass(ShortenReq.class);
            if (body == null || body.url == null)
                throw new BadRequestResponse("missing url");
            String normalized = normalizeUrl(body.url);
            String code = store.put(normalized);
            ctx.json(new ShortenRes(baseUrl + "/" + code));
        });

        app.get("/{code}", ctx -> {
            String code = ctx.pathParam("code");
            String url = store.get(code);
            if (url == null)
                throw new NotFoundResponse("unknown code");
            ctx.redirect(url);
        });

        app.get("/healthz", ctx -> ctx.result("ok"));

        app.start(config.port());
    }

    static String normalizeUrl(String url) {
        try {
            URI u = new URI(url);
            if (u.getScheme() == null) {
                u = new URI("https://" + url);
            }
            var norm = new URI(
                    u.getScheme().toLowerCase(),
                    u.getUserInfo(),
                    u.getHost(),
                    u.getPort(),
                    (u.getPath() == null || u.getPath().isEmpty()) ? "/" : u.getPath(),
                    u.getQuery(),
                    u.getFragment());
            return norm.toString();
        } catch (URISyntaxException e) {
            throw new BadRequestResponse("invalid url");
        }
    }

    static class UrlStore {
        private final Map<String, String> codeToUrl = new ConcurrentHashMap<>();
        private final Map<String, String> urlToCode = new ConcurrentHashMap<>();
        private final AtomicLong seq = new AtomicLong(125L);

        String put(String url) {
            var existing = urlToCode.get(url);
            if (existing != null)
                return existing;
            long id = seq.incrementAndGet();
            String code = toBase62(id);
            codeToUrl.put(code, url);
            urlToCode.put(url, code);
            return code;
        }

        String get(String code) {
            return codeToUrl.get(code);
        }

        private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                .toCharArray();

        static String toBase62(long n) {
            if (n == 0)
                return "0";
            StringBuilder sb = new StringBuilder();
            while (n > 0) {
                sb.append(ALPHABET[(int) (n % 62)]);
                n /= 62;
            }
            return sb.reverse().toString();
        }
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
