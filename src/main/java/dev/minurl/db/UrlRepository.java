package dev.minurl.db;

import java.util.Optional;

public interface UrlRepository {
    Optional<String> findNormalizedUrlByCode(String code);

    Optional<String> findCodeByNormalizedUrl(String normalizedUrl);

    boolean insert(String code, String normalizedUrl, String createdBy);
}
