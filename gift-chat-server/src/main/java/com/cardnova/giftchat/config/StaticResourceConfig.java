package com.cardnova.giftchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final Path imageDir;
    private final Path voiceDir;
    private final Path videoDir;

    public StaticResourceConfig(
        @Value("${app.upload.image-dir}") String imageDir,
        @Value("${app.upload.voice-dir}") String voiceDir,
        @Value("${app.upload.video-dir}") String videoDir
    ) {
        this.imageDir = Paths.get(imageDir).toAbsolutePath().normalize();
        this.voiceDir = Paths.get(voiceDir).toAbsolutePath().normalize();
        this.videoDir = Paths.get(videoDir).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/images/**")
            .addResourceLocations(imageDir.toUri().toString() + "/");
        registry.addResourceHandler("/uploads/voices/**")
            .addResourceLocations(voiceDir.toUri().toString() + "/");
        registry.addResourceHandler("/uploads/videos/**")
            .addResourceLocations(videoDir.toUri().toString() + "/");
    }
}
