package dev.minurl;

import java.io.ByteArrayOutputStream;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import io.javalin.http.BadRequestResponse;

public class UrlNormalizer {
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
    private static final Set<String> TRACKING_PARAMS = Set.of(
            "gclid",
            "gclsrc",
            "fbclid",
            "mc_eid",
            "mc_cid",
            "oly_anon_id",
            "oly_enc_id",
            "yclid",
            "_hsenc",
            "_hsmi",
            "mkt_tok",
            "igshid",
            "ref",
            "aff",
            "affid",
            "utm",
            "sc_campaign",
            "sc_channel",
            "sc_content",
            "sc_medium",
            "sc_outcome",
            "sc_geo");

    public String normalize(String inputUrl) {
        if (inputUrl == null) {
            throw new BadRequestResponse("missing url");
        }
        String trimmed = inputUrl.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestResponse("missing url");
        }

        URI parsed = parse(trimmed);
        parsed = enforceSupportedScheme(parsed);

        String scheme = parsed.getScheme().toLowerCase(Locale.ROOT);
        String host = normalizeHost(parsed);
        int port = normalizePort(parsed);
        String path = normalizePath(parsed);
        String query = normalizeQuery(parsed);
        String userInfo = parsed.getUserInfo();

        try {
            URI normalized = new URI(scheme, userInfo, host, port, path, query, null);
            return normalized.toASCIIString();
        } catch (URISyntaxException ex) {
            throw new BadRequestResponse("invalid url");
        }
    }

    private static URI parse(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            throw new BadRequestResponse("invalid url");
        }
    }

    private static URI enforceSupportedScheme(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if ("http".equals(scheme) || "https".equals(scheme)) {
            return uri;
        }
        throw new BadRequestResponse("missing or unsupported scheme");
    }

    private static String normalizeHost(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestResponse("invalid url");
        }
        String ascii = IDN.toASCII(host);
        return ascii.toLowerCase(Locale.ROOT);
    }

    private static int normalizePort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            return -1;
        }
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme) && port == 443) {
            return -1;
        }
        if ("http".equalsIgnoreCase(scheme) && port == 80) {
            return -1;
        }
        return port;
    }

    private static String normalizePath(URI uri) {
        String rawPath = uri.getRawPath();
        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }
        List<String> segments = new ArrayList<>();
        for (String segment : rawPath.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            segments.add(percentDecode(segment, false));
        }
        if (segments.isEmpty()) {
            return "/";
        }
        String joined = segments.stream()
                .map(UrlNormalizer::encodePathSegment)
                .collect(Collectors.joining("/"));
        String normalized = "/" + joined;
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeQuery(URI uri) {
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return null;
        }
        String[] parts = rawQuery.split("&");
        List<String> normalizedParts = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            String name;
            String value = null;
            int eq = part.indexOf('=');
            if (eq >= 0) {
                name = part.substring(0, eq);
                value = part.substring(eq + 1);
            } else {
                name = part;
            }
            String decodedName = percentDecode(name, true);
            if (isTrackingParameter(decodedName)) {
                continue;
            }
            String encodedName = encodeQueryComponent(decodedName);
            if (value == null) {
                normalizedParts.add(encodedName);
            } else {
                String decodedValue = percentDecode(value, true);
                String encodedValue = encodeQueryComponent(decodedValue);
                normalizedParts.add(encodedName + "=" + encodedValue);
            }
        }
        if (normalizedParts.isEmpty()) {
            return null;
        }
        normalizedParts.sort(String::compareTo);
        return String.join("&", normalizedParts);
    }

    private static boolean isTrackingParameter(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        return lowerName.startsWith("utm_") || TRACKING_PARAMS.contains(lowerName);
    }

    private static String encodePathSegment(String segment) {
        byte[] bytes = segment.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (isUnreserved(value)) {
                result.append((char) value);
            } else {
                appendEncodedByte(result, value);
            }
        }
        return result.toString();
    }

    private static String encodeQueryComponent(String component) {
        byte[] bytes = component.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (isUnreserved(value)) {
                result.append((char) value);
            } else if (value == ' ') {
                result.append("%20");
            } else {
                appendEncodedByte(result, value);
            }
        }
        return result.toString();
    }

    private static void appendEncodedByte(StringBuilder builder, int value) {
        builder.append('%')
                .append(HEX_DIGITS[(value >> 4) & 0x0F])
                .append(HEX_DIGITS[value & 0x0F]);
    }

    private static boolean isUnreserved(int value) {
        return (value >= 'A' && value <= 'Z')
                || (value >= 'a' && value <= 'z')
                || (value >= '0' && value <= '9')
                || value == '-'
                || value == '.'
                || value == '_'
                || value == '~';
    }

    private static String percentDecode(String raw, boolean plusAsSpace) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(raw.length());
        StringBuilder result = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length();) {
            char c = raw.charAt(i);
            if (c == '%') {
                buffer.reset();
                while (i < raw.length() && raw.charAt(i) == '%') {
                    if (i + 2 >= raw.length()) {
                        throw new BadRequestResponse("invalid url");
                    }
                    int hi = Character.digit(raw.charAt(i + 1), 16);
                    int lo = Character.digit(raw.charAt(i + 2), 16);
                    if (hi == -1 || lo == -1) {
                        throw new BadRequestResponse("invalid url");
                    }
                    buffer.write((hi << 4) + lo);
                    i += 3;
                }
                result.append(buffer.toString(StandardCharsets.UTF_8));
            } else {
                if (plusAsSpace && c == '+') {
                    result.append(' ');
                } else {
                    result.append(c);
                }
                i++;
            }
        }
        return result.toString();
    }
}
