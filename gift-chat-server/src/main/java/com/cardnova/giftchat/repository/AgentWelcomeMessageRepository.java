package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.AgentWelcomeMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentWelcomeMessageRepository extends JpaRepository<AgentWelcomeMessageEntity, String> {

    Optional<AgentWelcomeMessageEntity> findByAgent_Id(String agentId);
}
