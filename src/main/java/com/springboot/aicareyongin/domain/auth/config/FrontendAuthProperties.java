package com.springboot.aicareyongin.domain.auth.config;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.frontend")
public record FrontendAuthProperties(String successRedirectUri, List<String> allowedOrigins) {
    public FrontendAuthProperties {
        if (successRedirectUri == null) successRedirectUri = "http://127.0.0.1:5173/home";
        URI target = validateHttpUri(successRedirectUri);
        if (target.getRawQuery() != null || target.getRawFragment() != null) {
            throw new IllegalArgumentException("Frontend redirect URI must not contain a query or fragment");
        }
        if (allowedOrigins == null) {
            allowedOrigins = List.of(target.getScheme() + "://" + target.getRawAuthority());
        } else {
            allowedOrigins = List.copyOf(allowedOrigins);
            for (String origin : allowedOrigins) {
                URI uri = validateHttpUri(origin);
                if ((uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                        || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                    throw new IllegalArgumentException("CORS origin must contain only scheme, host, and port");
                }
            }
        }
    }

    private static URI validateHttpUri(String value) {
        URI uri = URI.create(value);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("Frontend URI must be an absolute HTTP(S) URL without user info");
        }
        return uri;
    }
}
