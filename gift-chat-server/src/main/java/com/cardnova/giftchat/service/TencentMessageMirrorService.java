package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.DirectMessageEntity;
import com.cardnova.giftchat.entity.SupportMessageEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.DirectMessageRepository;
import com.cardnova.giftchat.repository.SupportMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TencentMessageMirrorService {

    private final TencentIdentityService tencentIdentityService;
    private final TencentChatClient tencentChatClient;
    private final SupportMessageRepository supportMessageRepository;
    private final DirectMessageRepository directMessageRepository;

    public TencentMessageMirrorService(
        TencentIdentityService tencentIdentityService,
        TencentChatClient tencentChatClient,
        SupportMessageRepository supportMessageRepository,
        DirectMessageRepository directMessageRepository
    ) {
        this.tencentIdentityService = tencentIdentityService;
        this.tencentChatClient = tencentChatClient;
        this.supportMessageRepository = supportMessageRepository;
        this.directMessageRepository = directMessageRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mirrorSupportMessage(String messageId) {
        SupportMessageEntity message = supportMessageRepository.findById(messageId).orElse(null);
        if (message == null || message.getSenderUser() == null) {
            return;
        }
        UserEntity from = message.getSenderUser();
        UserEntity to = "AGENT".equalsIgnoreCase(from.getRoleCode())
            ? message.getConversation().getCustomerUser()
            : message.getConversation().getAssignedAgent();
        mirror(message, from, to);
        supportMessageRepository.save(message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mirrorDirectMessage(String messageId) {
        DirectMessageEntity message = directMessageRepository.findById(messageId).orElse(null);
        if (message == null || message.getSenderUser() == null) {
            return;
        }
        UserEntity from = message.getSenderUser();
        UserEntity to = message.getFriendship().getRequesterUser().getId().equals(from.getId())
            ? message.getFriendship().getAddresseeUser()
            : message.getFriendship().getRequesterUser();
        mirror(message, from, to);
        directMessageRepository.save(message);
    }

    private void mirror(SupportMessageEntity message, UserEntity from, UserEntity to) {
        MirrorResult result = send(from, to, message.getMessageType(), message.getContent());
        applyResult(message, result);
    }

    private void mirror(DirectMessageEntity message, UserEntity from, UserEntity to) {
        MirrorResult result = send(from, to, message.getMessageType(), message.getContent());
        applyResult(message, result);
    }

    private MirrorResult send(UserEntity from, UserEntity to, String messageType, String content) {
        if (!tencentChatClient.configured()) {
            return MirrorResult.skipped("Tencent Chat is disabled or not configured");
        }
        if (to == null) {
            return MirrorResult.skipped("Tencent receiver is not assigned");
        }
        String fromUserId = tencentIdentityService.ensureTencentUserId(from);
        String toUserId = tencentIdentityService.ensureTencentUserId(to);
        TencentChatClient.TencentChatResult importFrom = tencentChatClient.importAccount(fromUserId, from.getUsername());
        if (importFrom.attempted() && !importFrom.success()) {
            return MirrorResult.failed(importFrom.error());
        }
        TencentChatClient.TencentChatResult importTo = tencentChatClient.importAccount(toUserId, to.getUsername());
        if (importTo.attempted() && !importTo.success()) {
            return MirrorResult.failed(importTo.error());
        }
        String text = normalizeTencentText(messageType, content);
        TencentChatClient.TencentChatResult sent = tencentChatClient.sendTextMessage(fromUserId, toUserId, text);
        if (!sent.attempted()) {
            return MirrorResult.skipped(sent.error());
        }
        return sent.success() ? MirrorResult.mirrored(sent.messageKey()) : MirrorResult.failed(sent.error());
    }

    private String normalizeTencentText(String messageType, String content) {
        if ("TEXT".equalsIgnoreCase(messageType)) {
            return content;
        }
        return "[" + messageType.toLowerCase() + "] " + content;
    }

    private void applyResult(SupportMessageEntity message, MirrorResult result) {
        message.setTencentMirrorStatus(result.status());
        message.setTencentMessageKey(result.messageKey());
        message.setTencentMirroredAt(result.mirroredAt());
        message.setTencentMirrorError(result.error());
    }

    private void applyResult(DirectMessageEntity message, MirrorResult result) {
        message.setTencentMirrorStatus(result.status());
        message.setTencentMessageKey(result.messageKey());
        message.setTencentMirroredAt(result.mirroredAt());
        message.setTencentMirrorError(result.error());
    }

    private record MirrorResult(String status, String messageKey, LocalDateTime mirroredAt, String error) {
        static MirrorResult mirrored(String messageKey) {
            return new MirrorResult("MIRRORED", messageKey, LocalDateTime.now(), "");
        }

        static MirrorResult failed(String error) {
            return new MirrorResult("FAILED", "", null, truncate(error));
        }

        static MirrorResult skipped(String reason) {
            return new MirrorResult("SKIPPED", "", null, truncate(reason));
        }

        private static String truncate(String value) {
            if (value == null) {
                return "";
            }
            return value.length() <= 255 ? value : value.substring(0, 255);
        }
    }
}
