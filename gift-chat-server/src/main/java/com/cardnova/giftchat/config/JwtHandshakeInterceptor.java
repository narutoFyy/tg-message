package com.cardnova.giftchat.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.cardnova.giftchat.api.UnauthorizedException;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.service.JwtService;
import com.cardnova.giftchat.service.WebSocketChannelAuthorizationService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final UriTemplate CHAT_URI_TEMPLATE = new UriTemplate("/ws/chat/{channelType}/{channelId}");

    private final JwtService jwtService;
    private final WebSocketChannelAuthorizationService channelAuthorizationService;

    public JwtHandshakeInterceptor(
        JwtService jwtService,
        WebSocketChannelAuthorizationService channelAuthorizationService
    ) {
        this.jwtService = jwtService;
        this.channelAuthorizationService = channelAuthorizationService;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            throw new UnauthorizedException("Unsupported websocket request");
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Missing websocket token");
        }

        DecodedJWT jwt = jwtService.verify(token.trim());
        Map<String, String> pathVariables = CHAT_URI_TEMPLATE.match(request.getURI().getPath());
        UserEntity user = channelAuthorizationService.requireAccess(
            jwt.getSubject(),
            pathVariables.get("channelType"),
            pathVariables.get("channelId")
        );
        attributes.put("userId", user.getId());
        attributes.put("username", user.getUsername());
        attributes.put("roleCode", user.getRoleCode());
        attributes.put("channelType", pathVariables.get("channelType"));
        attributes.put("channelId", pathVariables.get("channelId"));
        return true;
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
        // no-op
    }
}
