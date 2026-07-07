package com.cardnova.giftchat.config;

import com.cardnova.giftchat.api.UnauthorizedException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final UriTemplate CHAT_URI_TEMPLATE = new UriTemplate("/ws/chat/{channelType}/{channelId}");

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        Map<String, String> pathVariables = CHAT_URI_TEMPLATE.match(request.getURI().getPath());
        if (pathVariables.isEmpty()) {
            throw new UnauthorizedException("Unsupported websocket request");
        }
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
