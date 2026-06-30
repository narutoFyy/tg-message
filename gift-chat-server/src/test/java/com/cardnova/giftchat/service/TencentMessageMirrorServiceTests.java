package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.SupportMessageEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.DirectMessageRepository;
import com.cardnova.giftchat.repository.SupportMessageRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMessageMirrorServiceTests {

    @Test
    void disabledTencentMirrorDoesNotCreateTencentIds() {
        Fixture fixture = fixture(false, TencentChatClient.TencentChatResult.skipped("disabled"));

        fixture.service.mirrorSupportMessage("message-1");

        assertEquals("SKIPPED", fixture.message.getTencentMirrorStatus());
        assertEquals("Tencent Chat is disabled or not configured", fixture.message.getTencentMirrorError());
        assertNull(fixture.user.getTencentUserId());
        verify(fixture.identityService, never()).ensureTencentUserId(any());
    }

    @Test
    void failedTencentMirrorIsRecordedWithoutThrowing() {
        Fixture fixture = fixture(true, TencentChatClient.TencentChatResult.failed("remote rejected"));

        fixture.service.mirrorSupportMessage("message-1");

        assertEquals("FAILED", fixture.message.getTencentMirrorStatus());
        assertEquals("remote rejected", fixture.message.getTencentMirrorError());
    }

    @Test
    void successfulTencentMirrorIsRecorded() {
        Fixture fixture = fixture(true, TencentChatClient.TencentChatResult.success("msg-key-1"));

        fixture.service.mirrorSupportMessage("message-1");

        assertEquals("MIRRORED", fixture.message.getTencentMirrorStatus());
        assertEquals("msg-key-1", fixture.message.getTencentMessageKey());
    }

    private Fixture fixture(boolean configured, TencentChatClient.TencentChatResult sendResult) {
        SupportMessageRepository supportRepository = mock(SupportMessageRepository.class);
        DirectMessageRepository directRepository = mock(DirectMessageRepository.class);
        TencentIdentityService identityService = mock(TencentIdentityService.class);
        TencentChatClient tencentChatClient = mock(TencentChatClient.class);

        UserEntity user = user("user-1", "cardnova_user", "USER");
        UserEntity agent = user("agent-1", "support_luna", "AGENT");
        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId("support-1");
        conversation.setCustomerUser(user);
        conversation.setAssignedAgent(agent);

        SupportMessageEntity message = new SupportMessageEntity();
        message.setId("message-1");
        message.setConversation(conversation);
        message.setSenderUser(user);
        message.setSenderRole("ME");
        message.setMessageType("TEXT");
        message.setContent("hello");
        message.setTencentMirrorStatus("PENDING");
        message.setCreatedAt(LocalDateTime.now());

        when(supportRepository.findById("message-1")).thenReturn(Optional.of(message));
        when(tencentChatClient.configured()).thenReturn(configured);
        when(identityService.ensureTencentUserId(user)).thenReturn("u_user1");
        when(identityService.ensureTencentUserId(agent)).thenReturn("u_agent1");
        when(tencentChatClient.importAccount(any(), any())).thenReturn(TencentChatClient.TencentChatResult.success(""));
        when(tencentChatClient.sendTextMessage(any(), any(), any())).thenReturn(sendResult);

        TencentMessageMirrorService service = new TencentMessageMirrorService(
            identityService,
            tencentChatClient,
            supportRepository,
            directRepository
        );
        return new Fixture(service, identityService, user, message);
    }

    private UserEntity user(String id, String username, String role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setRoleCode(role);
        return user;
    }

    private record Fixture(
        TencentMessageMirrorService service,
        TencentIdentityService identityService,
        UserEntity user,
        SupportMessageEntity message
    ) {
    }
}
