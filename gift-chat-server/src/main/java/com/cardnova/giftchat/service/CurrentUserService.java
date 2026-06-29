package com.cardnova.giftchat.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.cardnova.giftchat.api.ForbiddenException;
import com.cardnova.giftchat.api.UnauthorizedException;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CurrentUserService {

    public static final String DEFAULT_DEMO_USERNAME = "cardnova_user";
    private static final String DEMO_USER_HEADER = "X-Demo-User";

    private final UserRepository userRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final JwtService jwtService;
    private final boolean demoFallback;

    public CurrentUserService(
        UserRepository userRepository,
        ObjectProvider<HttpServletRequest> requestProvider,
        JwtService jwtService,
        @Value("${app.auth.demo-fallback}") boolean demoFallback
    ) {
        this.userRepository = userRepository;
        this.requestProvider = requestProvider;
        this.jwtService = jwtService;
        this.demoFallback = demoFallback;
    }

    public UserEntity getCurrentUser() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return getDefaultUser();
        }

        if (demoFallback) {
            String demoUsername = request.getHeader(DEMO_USER_HEADER);
            if (StringUtils.hasText(demoUsername)) {
                return userRepository.findByUsername(demoUsername.trim())
                    .orElseThrow(() -> new UnauthorizedException("Demo user not found"));
            }
        }

        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            return resolveByAuthorizationHeader(authorization.trim());
        }

        if (demoFallback) {
            return getDefaultUser();
        }

        throw new UnauthorizedException("Missing access token");
    }

    public void requireAgent(UserEntity user) {
        if (!"AGENT".equalsIgnoreCase(user.getRoleCode())) {
            throw new ForbiddenException("Agent access required");
        }
    }

    public void requireAdmin(UserEntity user) {
        if (!"ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            throw new ForbiddenException("Admin access required");
        }
    }

    public void requireAgentOrAdmin(UserEntity user) {
        if (!"AGENT".equalsIgnoreCase(user.getRoleCode()) && !"ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            throw new ForbiddenException("Agent or admin access required");
        }
    }

    private UserEntity resolveByAuthorizationHeader(String authorization) {
        if (!authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Unsupported authorization scheme");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("Access token is empty");
        }

        DecodedJWT decodedJWT = jwtService.verify(token);
        return userRepository.findById(decodedJWT.getSubject())
            .orElseThrow(() -> new UnauthorizedException("Token user not found"));
    }

    private UserEntity getDefaultUser() {
        return userRepository.findByUsername(DEFAULT_DEMO_USERNAME)
            .orElseThrow(() -> new IllegalStateException("Default demo user not found"));
    }
}
