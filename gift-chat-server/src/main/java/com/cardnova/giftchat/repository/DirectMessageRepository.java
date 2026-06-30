package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.DirectMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DirectMessageRepository extends JpaRepository<DirectMessageEntity, String> {

    List<DirectMessageEntity> findByFriendship_IdOrderByCreatedAtAsc(String friendshipId);

    List<DirectMessageEntity> findTop50ByTencentMirrorStatusOrderByCreatedAtAsc(String tencentMirrorStatus);

    Optional<DirectMessageEntity> findBySenderUser_IdAndClientMessageId(String senderUserId, String clientMessageId);

    @Query("select coalesce(max(message.serverSeq), 0) from DirectMessageEntity message where message.friendship.id = :friendshipId")
    long findMaxServerSeqByFriendshipId(String friendshipId);

    @Query("""
        select message from DirectMessageEntity message
        where message.friendship.id = :friendshipId
            and message.serverSeq > :sinceSeq
        order by message.serverSeq asc, message.createdAt asc
        """)
    List<DirectMessageEntity> findByFriendshipIdSinceSeq(String friendshipId, long sinceSeq);

    @Query("""
        select coalesce(max(message.serverSeq), 0) from DirectMessageEntity message
        where message.friendship.id = :friendshipId
            and message.createdAt <= :readAt
        """)
    long findReadSeqByFriendshipId(String friendshipId, LocalDateTime readAt);
}
