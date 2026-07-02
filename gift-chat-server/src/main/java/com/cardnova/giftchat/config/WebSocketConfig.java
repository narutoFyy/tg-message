package com.cardnova.giftchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(
        ChatWebSocketHandler chatWebSocketHandler,
        JwtHandshakeInterceptor jwtHandshakeInterceptor,
        @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat/{channelType}/{channelId}")
            .addInterceptors(jwtHandshakeInterceptor)
            .setAllowedOriginPatterns(trimmedOrigins());
    }

    private String[] trimmedOrigins() {
        String[] result = new String[allowedOrigins.length];
        for (int index = 0; index < allowedOrigins.length; index++) {
            result[index] = allowedOrigins[index].trim();
        }
        return result;
    }
}
