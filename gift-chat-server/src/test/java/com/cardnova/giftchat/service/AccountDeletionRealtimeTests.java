package com.cardnova.giftchat.service;

import com.cardnova.giftchat.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountDeletionRealtimeTests {
    @Test
    void fanoutClosesInactiveViewerBeforeSendingData() throws Exception {
        UserRepository users = mock(UserRepository.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("fixture-session");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of("userId", "deleted-user"));
        RealtimeChatService realtime = new RealtimeChatService(new ObjectMapper(), mock(RealtimeRedisBridge.class), users);
        realtime.register("support:fixture", session);
        realtime.broadcastPayloadLocal("support:fixture", Map.of("content", "private message"));
        verify(session).close();
        verify(session, never()).sendMessage(any());
    }

    @Test
    void fanoutContinuesForActiveViewer() throws Exception {
        UserRepository users = mock(UserRepository.class);
        when(users.existsByIdAndStatusCodeAndDeletedAtIsNull("active-user", "ACTIVE")).thenReturn(true);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("fixture-session");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of("userId", "active-user"));
        RealtimeChatService realtime = new RealtimeChatService(new ObjectMapper(), mock(RealtimeRedisBridge.class), users);
        realtime.register("support:fixture", session);
        realtime.broadcastPayloadLocal("support:fixture", Map.of("content", "private message"));
        verify(session).sendMessage(any());
        verify(session, never()).close();
    }
}
