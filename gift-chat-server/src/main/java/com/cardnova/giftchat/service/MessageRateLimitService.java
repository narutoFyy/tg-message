package com.cardnova.giftchat.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class MessageRateLimitService {

    private static final int MAX_MESSAGES_PER_WINDOW = 20;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Deque<Instant>> sendWindows = new ConcurrentHashMap<>();

    public void checkSendAllowed(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);
        Deque<Instant> window = sendWindows.computeIfAbsent(userId, ignored -> new ConcurrentLinkedDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                window.pollFirst();
            }
            if (window.size() >= MAX_MESSAGES_PER_WINDOW) {
                throw new IllegalArgumentException("Too many messages, please wait a moment");
            }
            window.addLast(now);
        }
    }
}
