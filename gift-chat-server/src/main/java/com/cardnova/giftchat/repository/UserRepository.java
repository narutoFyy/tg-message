package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByPhone(String phone);

    @Query("""
        select user from UserEntity user
        where replace(replace(replace(replace(replace(user.phone, ' ', ''), '-', ''), '(', ''), ')', ''), '.', '') = :phone
        """)
    Optional<UserEntity> findByNormalizedPhone(@Param("phone") String phone);

    Optional<UserEntity> findByInviteCode(String inviteCode);

    List<UserEntity> findByReferredByUserId(String referredByUserId);

    List<UserEntity> findByRoleCodeOrderByCreatedAtDesc(String roleCode);

    List<UserEntity> findByRoleCodeAndStatusCodeOrderByCreatedAtAsc(String roleCode, String statusCode);
}
