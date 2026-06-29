package com.cardnova.giftchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOriginPatterns(trimmedOrigins())
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*");

                registry.addMapping("/uploads/**")
                    .allowedOriginPatterns(trimmedOrigins())
                    .allowedMethods("GET", "OPTIONS")
                    .allowedHeaders("*");
            }
        };
    }

    private String[] trimmedOrigins() {
        String[] result = new String[allowedOrigins.length];
        for (int index = 0; index < allowedOrigins.length; index++) {
            result[index] = allowedOrigins[index].trim();
        }
        return result;
    }
}
