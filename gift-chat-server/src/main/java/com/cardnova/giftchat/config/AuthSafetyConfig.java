package com.cardnova.giftchat.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
public class AuthSafetyConfig {

    private static final String DEFAULT_JWT_SECRET = "change-me-in-production";
    private static final int MIN_PRODUCTION_JWT_SECRET_LENGTH = 32;
    private static final Set<String> FORBIDDEN_PRODUCTION_JWT_SECRETS = Set.of(
        DEFAULT_JWT_SECRET,
        "dev-only-local-jwt-secret-please-do-not-use-in-production",
        "replace-with-a-long-random-secret",
        "replace-with-your-jwt-secret"
    );

    private final Environment environment;
    private final String jwtSecret;
    private final String allowedOrigins;
    private final boolean demoFallback;

    public AuthSafetyConfig(
        Environment environment,
        @Value("${app.auth.jwt-secret}") String jwtSecret,
        @Value("${app.cors.allowed-origins}") String allowedOrigins,
        @Value("${app.auth.demo-fallback}") boolean demoFallback
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
        this.allowedOrigins = allowedOrigins == null ? "" : allowedOrigins.trim();
        this.demoFallback = demoFallback;
    }

    @PostConstruct
    public void validateProductionAuthSettings() {
        boolean developmentProfile = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> "dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile));
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
        boolean productionLike = productionProfile || !developmentProfile;
        if (!productionLike) {
            return;
        }

        validateJwtSecret();
        validateDemoFallback();
        validateCorsOrigins();
    }

    private void validateJwtSecret() {
        String normalized = jwtSecret.toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(jwtSecret)
            || jwtSecret.length() < MIN_PRODUCTION_JWT_SECRET_LENGTH
            || FORBIDDEN_PRODUCTION_JWT_SECRETS.contains(normalized)
            || normalized.contains("change-me")
            || normalized.contains("dev-only")
            || normalized.contains("local")
            || normalized.contains("replace-with")) {
            throw new IllegalStateException("APP_AUTH_JWT_SECRET must be a strong non-placeholder value outside dev/test");
        }
    }

    private void validateDemoFallback() {
        if (demoFallback) {
            throw new IllegalStateException("APP_AUTH_DEMO_FALLBACK must be false outside dev/test");
        }
    }

    private void validateCorsOrigins() {
        if (!StringUtils.hasText(allowedOrigins)) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must be set outside dev/test");
        }

        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .map(origin -> origin.toLowerCase(Locale.ROOT))
            .forEach(origin -> {
                if (!StringUtils.hasText(origin)
                    || "*".equals(origin)
                    || origin.contains("localhost")
                    || origin.contains("127.0.0.1")
                    || origin.contains("your-domain")
                    || origin.contains("your-server-ip")
                    || origin.contains("cpolar.top")) {
                    throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must contain only real production origins outside dev/test");
                }
            });
    }
}
