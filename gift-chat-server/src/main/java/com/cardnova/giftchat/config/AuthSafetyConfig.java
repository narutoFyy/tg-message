package com.cardnova.giftchat.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AuthSafetyConfig {

    private static final String DEFAULT_JWT_SECRET = "change-me-in-production";

    private final Environment environment;
    private final String jwtSecret;

    public AuthSafetyConfig(
        Environment environment,
        @Value("${app.auth.jwt-secret}") String jwtSecret
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
    }

    @PostConstruct
    public void validateProductionAuthSettings() {
        boolean developmentProfile = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> "dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile));
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
        if ((productionProfile || !developmentProfile) && DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("APP_AUTH_JWT_SECRET must be changed outside dev/test");
        }
    }
}
