package dev.minurl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dev.minurl.util.Base62;

public final class DeterministicCodeGenerator {
    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest not available", ex);
        }
    });

    public String hash(String normalizedUrl) {
        MessageDigest digest = SHA_256.get();
        digest.reset();
        byte[] hashed = digest.digest(normalizedUrl.getBytes(StandardCharsets.UTF_8));
        return Base62.encode(hashed);
    }
}
