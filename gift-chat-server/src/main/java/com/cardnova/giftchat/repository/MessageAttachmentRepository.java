package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.MessageAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachmentEntity, String> {
    List<MessageAttachmentEntity> findByOwnerMessageTypeAndOwnerMessageIdOrderBySortOrderAsc(String ownerMessageType, String ownerMessageId);

    List<MessageAttachmentEntity> findByOwnerMessageTypeAndOwnerMessageIdInOrderByOwnerMessageIdAscSortOrderAsc(
        String ownerMessageType,
        Collection<String> ownerMessageIds
    );
}
