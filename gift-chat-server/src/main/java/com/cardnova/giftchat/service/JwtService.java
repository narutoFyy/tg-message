package com.cardnova.giftchat.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.cardnova.giftchat.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long accessTokenMinutes;

    public JwtService(
        @Value("${app.auth.jwt-secret}") String secret,
        @Value("${app.auth.access-token-minutes}") long accessTokenMinutes
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer("gift-chat-server").build();
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String issueAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        return JWT.create()
            .withIssuer("gift-chat-server")
            .withSubject(user.getId())
            .withClaim("username", user.getUsername())
            .withClaim("role", user.getRoleCode())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm);
    }

    public Instant getAccessTokenExpiry() {
        return Instant.now().plus(accessTokenMinutes, ChronoUnit.MINUTES);
    }

    public DecodedJWT verify(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException exception) {
            throw new com.cardnova.giftchat.api.UnauthorizedException("Invalid access token");
        }
    }
}
