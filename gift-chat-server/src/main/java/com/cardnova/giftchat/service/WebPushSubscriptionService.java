package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.WebPushProperties;
import com.cardnova.giftchat.dto.RegisterWebPushSubscriptionRequest;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WebPushSubscriptionEntity;
import com.cardnova.giftchat.model.WebPushConfiguration;
import com.cardnova.giftchat.model.WebPushSubscriptionItem;
import com.cardnova.giftchat.repository.WebPushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Service
public class WebPushSubscriptionService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WebPushSubscriptionRepository repository;
    private final CurrentUserService currentUserService;
    private final WebPushProperties properties;

    public WebPushSubscriptionService(
        WebPushSubscriptionRepository repository,
        CurrentUserService currentUserService,
        WebPushProperties properties
    ) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.properties = properties;
    }

    public WebPushConfiguration configuration() {
        return new WebPushConfiguration(properties.configured(), properties.configured() ? properties.publicKey() : "");
    }

    @Transactional
    public WebPushSubscriptionItem register(RegisterWebPushSubscriptionRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String endpoint = request.endpoint().trim();
        String id = subscriptionId(endpoint);
        LocalDateTime now = LocalDateTime.now();
        WebPushSubscriptionEntity subscription = repository.findById(id).orElseGet(() -> {
            WebPushSubscriptionEntity created = new WebPushSubscriptionEntity();
            created.setId(id);
            created.setCreatedAt(now);
            return created;
        });
        subscription.setUser(currentUser);
        subscription.setEndpoint(endpoint);
        subscription.setP256dhKey(request.keys().p256dh().trim());
        subscription.setAuthKey(request.keys().auth().trim());
        subscription.setUserAgent(normalizeUserAgent(request.userAgent()));
        subscription.setEnabled(true);
        subscription.setLastSeenAt(now);
        subscription.setUpdatedAt(now);
        return toItem(repository.save(subscription));
    }

    @Transactional
    public boolean disable(String endpoint) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return repository.findById(subscriptionId(endpoint.trim()))
            .filter(subscription -> subscription.getUser().getId().equals(currentUser.getId()))
            .map(subscription -> {
                subscription.setEnabled(false);
                subscription.setUpdatedAt(LocalDateTime.now());
                repository.save(subscription);
                return true;
            })
            .orElse(false);
    }

    static String subscriptionId(String endpoint) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8));
            return "wps_" + HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Web Push subscription id generation failed", exception);
        }
    }

    private String normalizeUserAgent(String userAgent) {
        String normalized = userAgent == null ? "" : userAgent.trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private WebPushSubscriptionItem toItem(WebPushSubscriptionEntity subscription) {
        return new WebPushSubscriptionItem(
            subscription.getId(),
            subscription.isEnabled(),
            TIME_FORMATTER.format(subscription.getUpdatedAt())
        );
    }
}
