package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.LoanApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplicationEntity, String> {

    List<LoanApplicationEntity> findByOwnerUser_IdOrderByUpdatedAtDesc(String ownerUserId);

    List<LoanApplicationEntity> findByAssignedAgent_IdOrderByUpdatedAtDesc(String assignedAgentId);

    List<LoanApplicationEntity> findAllByOrderByUpdatedAtDesc();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
