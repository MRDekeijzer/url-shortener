package dev.minurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.http.BadRequestResponse;

class UrlNormalizerTest {
    private UrlNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new UrlNormalizer();
    }

    @Test
    void normalizeHost_lowercases_and_preserves_punycode() {
        String normalized = normalizer.normalize("https://XN--TST-QLA.DE/path");
        assertEquals("https://xn--tst-qla.de/path", normalized);
    }

    @Test
    void normalizeHost_missing_host_throws_bad_request() {
        assertThrows(BadRequestResponse.class, () -> normalizer.normalize("https:///path"));
    }

    @Test
    void normalizePort_strips_default_https_port() {
        String normalized = normalizer.normalize("https://Example.com:443/foo");
        assertEquals("https://example.com/foo", normalized);
    }

    @Test
    void normalizePort_strips_default_http_port() {
        String normalized = normalizer.normalize("http://Example.com:80");
        assertEquals("http://example.com/", normalized);
    }

    @Test
    void normalizePort_preserves_non_default_port() {
        String normalized = normalizer.normalize("http://example.com:8080/");
        assertEquals("http://example.com:8080/", normalized);
    }

    @Test
    void normalizePath_collapses_duplicate_segments_and_trailing_slash() {
        String normalized = normalizer.normalize("https://example.com//foo//bar/");
        assertEquals("https://example.com/foo/bar", normalized);
    }

    @Test
    void normalizePath_decodes_and_reencodes_segments() {
        String normalized = normalizer.normalize("https://example.com/%7Euser/Hello%20World");
        assertEquals("https://example.com/~user/Hello%2520World", normalized);
    }

    @Test
    void normalizeQuery_sorts_parameters_and_encodes_spaces() {
        String normalized = normalizer.normalize("https://example.com/path?b=hi+there&&c&a=1");
        assertEquals("https://example.com/path?a=1&b=hi%2520there&c", normalized);
    }

    @Test
    void normalizeQuery_strips_known_tracking_parameters() {
        String normalized = normalizer.normalize("https://example.com/path?utm_source=google&fbclid=abc&keep=1");
        assertEquals("https://example.com/path?keep=1", normalized);
    }

    @Test
    void normalizeQuery_drops_query_when_all_parameters_removed() {
        String normalized = normalizer.normalize("https://example.com/path?utm_source=google&fbclid=abc");
        assertEquals("https://example.com/path", normalized);
    }

    @Test
    void normalizeQuery_invalid_percent_encoding_throws_bad_request() {
        assertThrows(BadRequestResponse.class, () -> normalizer.normalize("https://example.com/path?bad=%ZZ"));
    }
}
