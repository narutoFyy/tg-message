package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.DirectMessageEntity;
import com.cardnova.giftchat.entity.SupportMessageEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.TencentMirrorFailureItem;
import com.cardnova.giftchat.repository.DirectMessageRepository;
import com.cardnova.giftchat.repository.SupportMessageRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ImOpsService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CurrentUserService currentUserService;
    private final SupportMessageRepository supportMessageRepository;
    private final DirectMessageRepository directMessageRepository;
    private final TencentMessageMirrorService tencentMessageMirrorService;

    public ImOpsService(
        CurrentUserService currentUserService,
        SupportMessageRepository supportMessageRepository,
        DirectMessageRepository directMessageRepository,
        TencentMessageMirrorService tencentMessageMirrorService
    ) {
        this.currentUserService = currentUserService;
        this.supportMessageRepository = supportMessageRepository;
        this.directMessageRepository = directMessageRepository;
        this.tencentMessageMirrorService = tencentMessageMirrorService;
    }

    public List<TencentMirrorFailureItem> failedTencentMirrors() {
        requireAdmin();
        return Stream.concat(
                supportMessageRepository.findTop50ByTencentMirrorStatusOrderByCreatedAtAsc("FAILED").stream()
                    .map(this::toFailureItem),
                directMessageRepository.findTop50ByTencentMirrorStatusOrderByCreatedAtAsc("FAILED").stream()
                    .map(this::toFailureItem)
            )
            .limit(100)
            .toList();
    }

    public int retryFailedTencentMirrors() {
        requireAdmin();
        List<TencentMirrorFailureItem> failures = failedTencentMirrors();
        failures.forEach(item -> {
            if ("support".equals(item.channelType())) {
                tencentMessageMirrorService.mirrorSupportMessage(item.messageId());
            } else if ("friend".equals(item.channelType())) {
                tencentMessageMirrorService.mirrorDirectMessage(item.messageId());
            }
        });
        return failures.size();
    }

    private TencentMirrorFailureItem toFailureItem(SupportMessageEntity message) {
        return new TencentMirrorFailureItem(
            message.getId(),
            "support",
            message.getConversation().getId(),
            message.getContent(),
            message.getTencentMirrorError() == null ? "" : message.getTencentMirrorError(),
            TIME_FORMATTER.format(message.getCreatedAt())
        );
    }

    private TencentMirrorFailureItem toFailureItem(DirectMessageEntity message) {
        return new TencentMirrorFailureItem(
            message.getId(),
            "friend",
            message.getFriendship().getId(),
            message.getContent(),
            message.getTencentMirrorError() == null ? "" : message.getTencentMirrorError(),
            TIME_FORMATTER.format(message.getCreatedAt())
        );
    }

    private void requireAdmin() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
    }
}
