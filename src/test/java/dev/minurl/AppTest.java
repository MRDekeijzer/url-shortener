package dev.minurl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AppTest {
    @Test
    void base62_monotonic() {
        assertEquals("Z", App.UrlStore.toBase62(61));
        assertEquals("10", App.UrlStore.toBase62(62));
    }

    @Test
    void normalization_adds_scheme_and_slash() {
        assertEquals("https://example.com/", App.normalizeUrl("example.com"));
        assertEquals("https://example.com/", App.normalizeUrl("https://example.com"));
    }
}
