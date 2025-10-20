package dev.minurl;

import java.util.Optional;

import dev.minurl.db.UrlRepository;
import io.javalin.http.NotFoundResponse;

public class UrlShortenerService {
    private final UrlRepository repository;
    private final UrlNormalizer normalizer;
    private final DeterministicCodeGenerator codeGenerator;
    private final int urlMinLength;

    public UrlShortenerService(UrlRepository repository,
            UrlNormalizer normalizer,
            DeterministicCodeGenerator codeGenerator,
            int urlMinLength) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.codeGenerator = codeGenerator;
        this.urlMinLength = urlMinLength;
    }

    public String shorten(String url) {
        String normalized = normalizer.normalize(url);
        return repository.findCodeByNormalizedUrl(normalized)
                .orElseGet(() -> createNewCode(normalized));
    }

    public String resolve(String code) {
        return repository.findNormalizedUrlByCode(code)
                .orElseThrow(() -> new NotFoundResponse("unknown code"));
    }

    private String createNewCode(String normalizedUrl) {
        String baseHash = codeGenerator.hash(normalizedUrl);
        for (int length = urlMinLength; length <= baseHash.length(); length++) {
            String candidate = baseHash.substring(0, length);
            if (repository.insert(candidate, normalizedUrl)) {
                return candidate;
            }
            Optional<String> existing = repository.findCodeByNormalizedUrl(normalizedUrl);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        throw new IllegalStateException("Unable to allocate collision-free short code");
    }
}
