package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.RateLimitException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageRateLimitServiceTests {

    @Test
    void supportLimitsAreRoleAwareAndIsolatedByAccount() {
        MessageRateLimitService service = new MessageRateLimitService();

        for (int index = 0; index < 20; index++) {
            service.checkSupportSendAllowed("user-1", "USER");
        }
        RateLimitException userLimit = assertThrows(
            RateLimitException.class,
            () -> service.checkSupportSendAllowed("user-1", "USER")
        );
        assertEquals("Message limit reached: up to 20 messages per minute.", userLimit.getMessage());
        assertDoesNotThrow(() -> service.checkSupportSendAllowed("user-2", "USER"));

        for (int index = 0; index < 120; index++) {
            service.checkSupportSendAllowed("agent-1", "AGENT");
        }
        RateLimitException staffLimit = assertThrows(
            RateLimitException.class,
            () -> service.checkSupportSendAllowed("agent-1", "AGENT")
        );
        assertEquals("Message limit reached: up to 120 messages per minute.", staffLimit.getMessage());
        assertDoesNotThrow(() -> service.checkSupportSendAllowed("admin-1", "ADMIN"));
    }

    @Test
    void directAndSupportWindowsDoNotConsumeEachOther() {
        MessageRateLimitService service = new MessageRateLimitService();

        for (int index = 0; index < 20; index++) {
            service.checkSendAllowed("same-user");
        }

        assertDoesNotThrow(() -> service.checkSupportSendAllowed("same-user", "USER"));
    }
}
