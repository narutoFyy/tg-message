package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    List<UserEntity> findByRoleCodeOrderByCreatedAtDesc(String roleCode);

    List<UserEntity> findByRoleCodeAndStatusCodeOrderByCreatedAtAsc(String roleCode, String statusCode);
}
