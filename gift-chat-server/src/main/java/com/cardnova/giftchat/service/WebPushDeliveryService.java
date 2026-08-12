package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.WebPushSubscriptionEntity;
import com.cardnova.giftchat.repository.WebPushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class WebPushDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPushDeliveryService.class);
    private static final String GENERIC_MESSAGE = "\u60a8\u6536\u5230\u4e86\u4e00\u6761\u6d88\u606f";

    private final WebPushSubscriptionRepository repository;
    private final WebPushClient client;
    private final ObjectMapper objectMapper;

    public WebPushDeliveryService(
        WebPushSubscriptionRepository repository,
        WebPushClient client,
        ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public void deliverSupportMessage(String userId, String conversationId) {
        if (!client.configured()) {
            return;
        }
        String payload = payload(conversationId);
        for (WebPushSubscriptionEntity subscription : repository.findByUser_IdAndEnabledTrue(userId)) {
            deliver(subscription, payload);
        }
    }

    private void deliver(WebPushSubscriptionEntity subscription, String payload) {
        try {
            int status = client.send(subscription, payload);
            if (status == 404 || status == 410) {
                disableExpired(subscription.getId());
            } else if (status < 200 || status >= 300) {
                LOGGER.warn("Web Push returned status {} for subscription {}", status, subscription.getId());
            }
        } catch (Exception exception) {
            LOGGER.warn("Web Push failed for subscription {}", subscription.getId(), exception);
        }
    }

    private String payload(String conversationId) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "title", "Xcard",
                "body", GENERIC_MESSAGE,
                "tag", "support-message",
                "url", "/#/pages/support/index",
                "conversationId", conversationId
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("Web Push payload serialization failed", exception);
        }
    }

    @Transactional
    void disableExpired(String subscriptionId) {
        repository.findById(subscriptionId).ifPresent(subscription -> {
            subscription.setEnabled(false);
            subscription.setUpdatedAt(LocalDateTime.now());
            repository.save(subscription);
        });
    }
}
