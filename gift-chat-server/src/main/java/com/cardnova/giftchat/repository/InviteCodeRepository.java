package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.InviteCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCodeEntity, String> {
    Optional<InviteCodeEntity> findByOwnerUser_Id(String ownerUserId);

    @Query("""
        select source
        from UserEntity user
        join user.registrationInviteCode source
        left join fetch source.ownerUser
        where user.id = :userId
        """)
    Optional<InviteCodeEntity> findRegistrationSourceByUserId(@Param("userId") String userId);

    List<InviteCodeEntity> findByCodeTypeOrderByCreatedAtDesc(String codeType);
}
