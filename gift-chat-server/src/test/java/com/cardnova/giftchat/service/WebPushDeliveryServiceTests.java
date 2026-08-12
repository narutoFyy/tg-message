package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.WebPushProperties;
import com.cardnova.giftchat.entity.WebPushSubscriptionEntity;
import com.cardnova.giftchat.repository.WebPushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

class WebPushDeliveryServiceTests {

    @Test
    void configuredVapidKeyPairInitializesClient() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        var pair = generator.generateKeyPair();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        WebPushProperties properties = new WebPushProperties(
            true,
            encoder.encodeToString(Utils.encode((ECPublicKey) pair.getPublic())),
            encoder.encodeToString(Utils.encode((ECPrivateKey) pair.getPrivate())),
            "mailto:test@localhost",
            300
        );

        WebPushClient client = new WebPushClient(properties);

        org.junit.jupiter.api.Assertions.assertNotNull(client.service());
    }

    @Test
    void missingConfigurationIsANoOp() {
        WebPushSubscriptionRepository repository = mock(WebPushSubscriptionRepository.class);
        WebPushClient client = mock(WebPushClient.class);
        when(client.configured()).thenReturn(false);
        WebPushDeliveryService service = new WebPushDeliveryService(repository, client, new ObjectMapper());

        service.deliverSupportMessage("user-1", "support-1");

        verify(repository, never()).findByUser_IdAndEnabledTrue("user-1");
    }

    @Test
    void pushFailureDoesNotEscapeDeliveryBoundary() throws Exception {
        WebPushSubscriptionRepository repository = mock(WebPushSubscriptionRepository.class);
        WebPushClient client = mock(WebPushClient.class);
        WebPushSubscriptionEntity subscription = subscription("wps_failure");
        when(client.configured()).thenReturn(true);
        when(repository.findByUser_IdAndEnabledTrue("user-1")).thenReturn(List.of(subscription));
        when(client.send(eq(subscription), anyString()))
            .thenThrow(new IllegalStateException("synthetic push failure"));
        WebPushDeliveryService service = new WebPushDeliveryService(repository, client, new ObjectMapper());

        assertDoesNotThrow(() -> service.deliverSupportMessage("user-1", "support-1"));
    }

    @Test
    void goneSubscriptionIsDisabled() throws Exception {
        WebPushSubscriptionRepository repository = mock(WebPushSubscriptionRepository.class);
        WebPushClient client = mock(WebPushClient.class);
        WebPushSubscriptionEntity subscription = subscription("wps_gone");
        when(client.configured()).thenReturn(true);
        when(repository.findByUser_IdAndEnabledTrue("user-1")).thenReturn(List.of(subscription));
        when(repository.findById("wps_gone")).thenReturn(Optional.of(subscription));
        when(client.send(eq(subscription), anyString())).thenReturn(410);
        WebPushDeliveryService service = new WebPushDeliveryService(repository, client, new ObjectMapper());

        service.deliverSupportMessage("user-1", "support-1");

        verify(repository).save(subscription);
        org.junit.jupiter.api.Assertions.assertFalse(subscription.isEnabled());
    }

    private WebPushSubscriptionEntity subscription(String id) {
        WebPushSubscriptionEntity subscription = new WebPushSubscriptionEntity();
        subscription.setId(id);
        subscription.setEnabled(true);
        return subscription;
    }
}
