package com.example.friend_microservice.repository;

import com.example.friend_microservice.entities.Friend;
import com.example.friend_microservice.enums.FriendsStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendsRepository extends JpaRepository<Friend, UUID>, JpaSpecificationExecutor<Friend> {
    Optional<Friend> findByUserId(UUID userId);
    Optional<Friend> findByUserIdAndFriendId(UUID userId, UUID friendId);
    List<Friend> findAllByUserId(UUID userId);
    List<Friend> findAllByFriendIdIn(List<UUID> friends);
    List<Friend> findAllByFriendId(UUID friendId);
    List<Friend> findAllByUserIdAndFriendsStatusCode(UUID userId, FriendsStatus friendsStatusCode);
    List<Friend> findAllByFriendIdAndFriendsStatusCode(UUID userId, FriendsStatus friendsStatusCode);
    @Transactional
    int deleteByUserIdAndFriendId(UUID userId, UUID friendId);
}
