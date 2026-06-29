package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.SupportConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportConversationRepository extends JpaRepository<SupportConversationEntity, String> {

    List<SupportConversationEntity> findByCustomerUser_IdOrderByUpdatedAtDesc(String customerUserId);

    List<SupportConversationEntity> findByAssignedAgent_IdOrderByUpdatedAtDesc(String assignedAgentId);

    List<SupportConversationEntity> findAllByOrderByUpdatedAtDesc();

    Optional<SupportConversationEntity> findFirstByCustomerUser_IdOrderByUpdatedAtDesc(String customerUserId);

    boolean existsByCustomerUser_IdAndAssignedAgent_Id(String customerUserId, String assignedAgentId);

    long countByAssignedAgent_Id(String assignedAgentId);

    long countByAssignedAgent_IdAndCustomerUser_StatusCode(String assignedAgentId, String statusCode);
}
