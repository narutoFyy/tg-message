package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.RateLimitException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

@Service
public class LoginRateLimitService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MAX_TRACKED_WINDOWS = 10_000;
    private static final int CLEANUP_INTERVAL = 100;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);
    private static final String RATE_LIMIT_MESSAGE = "Too many login attempts, please try again later";
    private static final String REDIS_FAILURE_PREFIX = "gift-chat:login-rate-limit:failures:";
    private static final String REDIS_BLOCK_PREFIX = "gift-chat:login-rate-limit:blocked:";

    private final Map<String, LoginWindow> loginWindows = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final boolean redisEnabled;

    public LoginRateLimitService(
        ObjectProvider<StringRedisTemplate> redisTemplateProvider,
        @Value("${app.login-rate-limit.redis.enabled:false}") boolean redisEnabled
    ) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisEnabled = redisEnabled;
    }

    public void checkAllowed(String identifier, String clientIp) {
        if (checkAllowedWithRedis(identifier, clientIp)) {
            return;
        }
        LoginWindow window = loginWindows.get(key(identifier, clientIp));
        if (window == null) {
            return;
        }
        synchronized (window) {
            Instant now = Instant.now();
            if (window.blockedUntil != null && window.blockedUntil.isAfter(now)) {
                throw new RateLimitException(RATE_LIMIT_MESSAGE);
            }
            prune(window, now);
        }
    }

    public void recordFailure(String identifier, String clientIp) {
        if (recordFailureWithRedis(identifier, clientIp)) {
            return;
        }
        Instant now = Instant.now();
        cleanupExpiredIfNeeded(now);
        String key = key(identifier, clientIp);
        if (!loginWindows.containsKey(key) && loginWindows.size() >= MAX_TRACKED_WINDOWS) {
            cleanupExpired(now);
            if (loginWindows.size() >= MAX_TRACKED_WINDOWS) {
                throw new RateLimitException(RATE_LIMIT_MESSAGE);
            }
        }
        LoginWindow window = loginWindows.computeIfAbsent(key, ignored -> new LoginWindow());
        synchronized (window) {
            prune(window, now);
            window.failures.addLast(now);
            if (window.failures.size() >= MAX_FAILED_ATTEMPTS) {
                window.blockedUntil = now.plus(BLOCK_DURATION);
            }
        }
    }

    public void recordSuccess(String identifier, String clientIp) {
        if (recordSuccessWithRedis(identifier, clientIp)) {
            return;
        }
        loginWindows.remove(key(identifier, clientIp));
    }

    private boolean checkAllowedWithRedis(String identifier, String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return false;
        }
        try {
            Boolean blocked = redisTemplate.hasKey(blockKey(identifier, clientIp));
            if (Boolean.TRUE.equals(blocked)) {
                throw new RateLimitException(RATE_LIMIT_MESSAGE);
            }
            return true;
        } catch (RateLimitException exception) {
            throw exception;
        } catch (RedisConnectionFailureException | RedisSystemException exception) {
            return false;
        }
    }

    private boolean recordFailureWithRedis(String identifier, String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return false;
        }
        try {
            String failureKey = failureKey(identifier, clientIp);
            Long failures = redisTemplate.opsForValue().increment(failureKey);
            if (failures != null && failures == 1L) {
                redisTemplate.expire(failureKey, WINDOW.toSeconds(), TimeUnit.SECONDS);
            }
            if (failures != null && failures >= MAX_FAILED_ATTEMPTS) {
                redisTemplate.opsForValue().set(blockKey(identifier, clientIp), "1", BLOCK_DURATION.toSeconds(), TimeUnit.SECONDS);
            }
            return true;
        } catch (RedisConnectionFailureException | RedisSystemException exception) {
            return false;
        }
    }

    private boolean recordSuccessWithRedis(String identifier, String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return false;
        }
        try {
            redisTemplate.delete(List.of(failureKey(identifier, clientIp), blockKey(identifier, clientIp)));
            return true;
        } catch (RedisConnectionFailureException | RedisSystemException exception) {
            return false;
        }
    }

    private StringRedisTemplate redisTemplate() {
        if (!redisEnabled) {
            return null;
        }
        return redisTemplateProvider.getIfAvailable();
    }

    private String failureKey(String identifier, String clientIp) {
        return REDIS_FAILURE_PREFIX + encodedKey(identifier, clientIp);
    }

    private String blockKey(String identifier, String clientIp) {
        return REDIS_BLOCK_PREFIX + encodedKey(identifier, clientIp);
    }

    private String encodedKey(String identifier, String clientIp) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key(identifier, clientIp).getBytes(StandardCharsets.UTF_8));
    }

    private void cleanupExpiredIfNeeded(Instant now) {
        if (cleanupCounter.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpired(now);
        }
    }

    private void cleanupExpired(Instant now) {
        loginWindows.forEach((key, window) -> {
            synchronized (window) {
                prune(window, now);
                if (window.failures.isEmpty() && window.blockedUntil == null) {
                    loginWindows.remove(key, window);
                }
            }
        });
    }

    private void prune(LoginWindow window, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!window.failures.isEmpty() && window.failures.peekFirst().isBefore(cutoff)) {
            window.failures.pollFirst();
        }
        if (window.blockedUntil != null && !window.blockedUntil.isAfter(now)) {
            window.blockedUntil = null;
        }
    }

    private String key(String identifier, String clientIp) {
        String normalizedIdentifier = StringUtils.hasText(identifier) ? identifier.trim().toLowerCase() : "unknown";
        String normalizedIp = StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
        return normalizedIdentifier + "|" + normalizedIp;
    }

    private static class LoginWindow {
        private final Deque<Instant> failures = new ConcurrentLinkedDeque<>();
        private Instant blockedUntil;
    }
}
