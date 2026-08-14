package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :userId")
    Optional<UserEntity> findByIdForUpdate(@Param("userId") String userId);

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

    Page<UserEntity> findByRegistrationInviteCode_CodeOrderByCreatedAtDesc(String inviteCode, Pageable pageable);

    long countByRegistrationInviteCode_Code(String inviteCode);

    List<UserEntity> findByRoleCodeOrderByCreatedAtDesc(String roleCode);

    @Query("""
        select user from UserEntity user
        where (:keyword is null or
            lower(user.username) like lower(concat('%', :keyword, '%')) or
            lower(coalesce(user.email, '')) like lower(concat('%', :keyword, '%')) or
            lower(coalesce(user.phone, '')) like lower(concat('%', :keyword, '%')))
          and (:role is null or user.roleCode = :role)
          and (:status is null or user.statusCode = :status)
        order by user.createdAt desc
        """)
    Page<UserEntity> searchAdminUsers(
        @Param("keyword") String keyword,
        @Param("role") String role,
        @Param("status") String status,
        Pageable pageable
    );

    List<UserEntity> findByRoleCodeAndStatusCodeOrderByCreatedAtAsc(String roleCode, String statusCode);
}
