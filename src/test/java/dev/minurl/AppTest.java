package dev.minurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.minurl.db.UrlRepository;

class AppTest {
    private static final int URL_MIN_LENGTH = 5;
    private UrlShortenerService service;
    private InMemoryUrlRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUrlRepository();
        service = new UrlShortenerService(
                repository,
                new UrlNormalizer(),
                new DeterministicCodeGenerator(),
                URL_MIN_LENGTH);
    }

    @Test
    void normalization_canonicalizes_scheme_and_host() {
        UrlNormalizer normalizer = new UrlNormalizer();
        assertEquals("https://example.com/", normalizer.normalize("HTTPS://example.com"));
        assertEquals("http://example.com/", normalizer.normalize("HTTP://Example.com"));
        assertEquals("https://example.com/path", normalizer.normalize("https://example.com/path/"));
    }

    @Test
    void generated_code_is_deterministic_sha_base62() {
        String first = service.shorten("https://example.com/");
        String second = service.shorten(" https://example.com ");
        assertEquals("3zwNn", first);
        assertEquals(first, second);
        assertEquals(1, repository.insertAttempts);
    }

    @Test
    void service_resolves_stored_url() {
        String code = service.shorten("https://sub.example.com:443/path?utm_source=x&ref=123");
        assertTrue(repository.codeToUrl.containsKey(code));
        assertEquals("https://sub.example.com/path", service.resolve(code));
    }

    private static final class InMemoryUrlRepository implements UrlRepository {
        private final Map<String, String> codeToUrl = new HashMap<>();
        private final Map<String, String> urlToCode = new HashMap<>();
        private int insertAttempts = 0;

        @Override
        public Optional<String> findNormalizedUrlByCode(String code) {
            return Optional.ofNullable(codeToUrl.get(code));
        }

        @Override
        public Optional<String> findCodeByNormalizedUrl(String normalizedUrl) {
            return Optional.ofNullable(urlToCode.get(normalizedUrl));
        }

        @Override
        public boolean insert(String code, String normalizedUrl) {
            insertAttempts++;
            if (codeToUrl.containsKey(code) || urlToCode.containsKey(normalizedUrl)) {
                return false;
            }
            codeToUrl.put(code, normalizedUrl);
            urlToCode.put(normalizedUrl, code);
            return true;
        }
    }
}
