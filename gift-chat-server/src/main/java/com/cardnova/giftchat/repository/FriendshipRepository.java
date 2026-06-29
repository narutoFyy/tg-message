package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.FriendshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, String> {

    List<FriendshipEntity> findByRequesterUser_IdOrAddresseeUser_IdOrderByUpdatedAtDesc(String requesterUserId, String addresseeUserId);

    List<FriendshipEntity> findByAddresseeUser_IdAndStatusCodeOrderByCreatedAtDesc(String addresseeUserId, String statusCode);

    List<FriendshipEntity> findByRequesterUser_IdAndStatusCodeOrderByCreatedAtDesc(String requesterUserId, String statusCode);

    boolean existsByRequesterUser_IdAndAddresseeUser_IdAndStatusCodeIn(String requesterUserId, String addresseeUserId, java.util.Collection<String> statuses);

    java.util.Optional<FriendshipEntity> findByRequesterUser_IdAndAddresseeUser_Id(String requesterUserId, String addresseeUserId);

    java.util.Optional<FriendshipEntity> findByAddresseeUser_IdAndRequesterUser_Id(String addresseeUserId, String requesterUserId);
}
