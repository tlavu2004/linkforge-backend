package com.tlavu.linkforge.domain.valueobject;

import com.tlavu.linkforge.domain.exception.InvalidUrlException;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public record OriginalUrl(String url) {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_LENGTH = 2048;

    public OriginalUrl {
        if (!StringUtils.hasText(url)) {
            throw new InvalidUrlException("URL cannot be empty");
        }
        if (url.length() > MAX_LENGTH) {
            throw new InvalidUrlException("URL exceeds maximum length of " + MAX_LENGTH);
        }
    }

    public static OriginalUrl of(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new InvalidUrlException("URL cannot be empty");
        }

        String normalized = rawUrl.trim();

        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                throw new InvalidUrlException("Invalid URL scheme. Only HTTP and HTTPS are allowed.");
            }

            if (!StringUtils.hasText(host)) {
                throw new InvalidUrlException("URL must contain a host.");
            }

            // Normalize scheme and host to lowercase, keep path/query as is
            // Reconstruct URI to ensuring clean format
            URI normalizedUri = new URI(
                    scheme.toLowerCase(),
                    uri.getUserInfo(),
                    host.toLowerCase(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());

            return new OriginalUrl(normalizedUri.toString());

        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Malformed URL: " + e.getMessage());
        }
    }
}
