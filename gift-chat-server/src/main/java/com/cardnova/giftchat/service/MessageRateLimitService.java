package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.RateLimitException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class MessageRateLimitService {

    private static final int USER_MESSAGES_PER_WINDOW = 20;
    private static final int STAFF_MESSAGES_PER_WINDOW = 120;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Deque<Instant>> sendWindows = new ConcurrentHashMap<>();

    public void checkSendAllowed(String userId) {
        checkSendAllowed("direct", userId, USER_MESSAGES_PER_WINDOW);
    }

    public void checkSupportSendAllowed(String userId, String roleCode) {
        boolean staff = "AGENT".equalsIgnoreCase(roleCode) || "ADMIN".equalsIgnoreCase(roleCode);
        checkSendAllowed("support", userId, staff ? STAFF_MESSAGES_PER_WINDOW : USER_MESSAGES_PER_WINDOW);
    }

    private void checkSendAllowed(String channel, String userId, int maximumMessages) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);
        String key = channel + ":" + userId;
        Deque<Instant> window = sendWindows.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }
            if (window.size() >= maximumMessages) {
                throw new RateLimitException(
                    "Message limit reached: up to %d messages per minute.".formatted(maximumMessages)
                );
            }
            window.addLast(now);
        }
    }
}
