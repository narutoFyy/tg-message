package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.RateLimitException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginRateLimitService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MAX_TRACKED_WINDOWS = 10_000;
    private static final int CLEANUP_INTERVAL = 100;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);
    private static final String RATE_LIMIT_MESSAGE = "Too many login attempts, please try again later";

    private final Map<String, LoginWindow> loginWindows = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    public void checkAllowed(String identifier, String clientIp) {
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
        loginWindows.remove(key(identifier, clientIp));
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
