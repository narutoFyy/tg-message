package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.SupportMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, String> {

    List<SupportMessageEntity> findByConversation_IdOrderByCreatedAtAsc(String conversationId);

    List<SupportMessageEntity> findTop50ByTencentMirrorStatusOrderByCreatedAtAsc(String tencentMirrorStatus);

    Optional<SupportMessageEntity> findBySenderUser_IdAndClientMessageId(String senderUserId, String clientMessageId);

    @Query("select coalesce(max(message.serverSeq), 0) from SupportMessageEntity message where message.conversation.id = :conversationId")
    long findMaxServerSeqByConversationId(String conversationId);

    @Query("""
        select message from SupportMessageEntity message
        where message.conversation.id = :conversationId
            and message.serverSeq > :sinceSeq
        order by message.serverSeq asc, message.createdAt asc
        """)
    List<SupportMessageEntity> findByConversationIdSinceSeq(String conversationId, long sinceSeq);

    @Query("""
        select coalesce(max(message.serverSeq), 0) from SupportMessageEntity message
        where message.conversation.id = :conversationId
            and message.createdAt <= :readAt
        """)
    long findReadSeqByConversationId(String conversationId, LocalDateTime readAt);
}
