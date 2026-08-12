package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.WebPushProperties;
import com.cardnova.giftchat.entity.WebPushSubscriptionEntity;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class WebPushClient {

    private final WebPushProperties properties;
    private volatile PushService pushService;

    public WebPushClient(WebPushProperties properties) {
        this.properties = properties;
    }

    public boolean configured() {
        return properties.configured();
    }

    public int send(WebPushSubscriptionEntity subscription, String payload) throws Exception {
        Notification notification = new Notification(
            subscription.getEndpoint(),
            subscription.getP256dhKey(),
            subscription.getAuthKey(),
            payload.getBytes(StandardCharsets.UTF_8),
            properties.ttlSeconds()
        );
        HttpResponse response = service().send(notification);
        try {
            return response.getStatusLine().getStatusCode();
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    PushService service() throws Exception {
        PushService current = pushService;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (pushService == null) {
                pushService = new PushService(
                    properties.publicKey(),
                    properties.privateKey(),
                    properties.subject()
                );
            }
            return pushService;
        }
    }
}
