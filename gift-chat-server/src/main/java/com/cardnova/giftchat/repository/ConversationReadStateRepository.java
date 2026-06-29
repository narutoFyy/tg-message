package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.ConversationReadStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConversationReadStateRepository extends JpaRepository<ConversationReadStateEntity, String> {

    Optional<ConversationReadStateEntity> findByConversationTypeAndConversationIdAndUser_Id(
        String conversationType,
        String conversationId,
        String userId
    );

    Optional<ConversationReadStateEntity> findByConversationTypeAndConversationIdAndUser_IdNot(
        String conversationType,
        String conversationId,
        String userId
    );

    @Modifying
    @Query(value = """
        INSERT INTO conversation_read_state (
            id,
            conversation_type,
            conversation_id,
            user_id,
            last_read_at,
            created_at,
            updated_at
        )
        VALUES (
            :id,
            :conversationType,
            :conversationId,
            :userId,
            :lastReadAt,
            :now,
            :now
        )
        ON DUPLICATE KEY UPDATE
            last_read_at = :lastReadAt,
            updated_at = :now
        """, nativeQuery = true)
    void upsertReadState(
        @Param("id") String id,
        @Param("conversationType") String conversationType,
        @Param("conversationId") String conversationId,
        @Param("userId") String userId,
        @Param("lastReadAt") LocalDateTime lastReadAt,
        @Param("now") LocalDateTime now
    );
}
