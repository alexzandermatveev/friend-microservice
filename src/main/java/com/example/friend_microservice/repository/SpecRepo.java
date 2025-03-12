package com.example.friend_microservice.repository;

import com.example.friend_microservice.entities.Friend;
import com.example.friend_microservice.enums.FriendsStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public final class SpecRepo {

    @PersistenceContext
    private final EntityManager entityManager;

    public Specification<Friend> getSpecByUserIdAndFriendStatus(UUID uuid, FriendsStatus status) {
        return (root, query, criteriaBuilder) -> {
            Predicate userIdPredicate = criteriaBuilder.equal(root.get("userId"), uuid);
            if (status == FriendsStatus.ALL) {
                return criteriaBuilder.and(userIdPredicate);
            }
            Predicate statusPredicate = criteriaBuilder.equal(root.get("friendsStatusCode"), status.toString());
            return criteriaBuilder.and(userIdPredicate, statusPredicate);
        };
    }

    /**
     * Возвращает Specification для нахождения друзей друзей
     */
    public List<UUID> getAnotherFriendsByFriendsToList(UUID currentUserId, List<UUID> userFriends) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UUID> query = criteriaBuilder.createQuery(UUID.class);
        Root<Friend> root = query.from(Friend.class);
        query.select(root.get("userId")).distinct(true);
        Predicate friendsFriends = root.get("friendId").in(userFriends);
        Predicate notCurrentUser = criteriaBuilder.notEqual(root.get("friendId"), currentUserId);
        Predicate deletedUsers = criteriaBuilder.isFalse(root.get("isDeleted"));
        Predicate excludeFriends = criteriaBuilder.not(root.get("userId").in(userFriends));
        Predicate excludeStatuses = criteriaBuilder.not(
                root.get("friendsStatusCode").in(
                        FriendsStatus.BLOCKED.name(),
                        FriendsStatus.DECLINED.name()
                )
        );

        query.where(criteriaBuilder.and(friendsFriends, notCurrentUser, excludeFriends, excludeStatuses, deletedUsers));

        TypedQuery<UUID> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }


    public Specification<Friend> getAnotherFriendsByFriends(UUID currentUserId, List<UUID> userFriends) {

        return (root, query, criteriaBuilder) -> {
            query.distinct(true); // находим только уникальные значения

            // Друзья друзей: friendId должен быть среди друзей текущего пользователя
            Predicate friendsFriends = root.get("friendId").in(userFriends);

            // Исключаем текущего пользователя
            Predicate notCurrentUser = criteriaBuilder.notEqual(root.get("friendId"), currentUserId);
            // исключаем сущности с удаленными пользователями
            Predicate deletedUsers = criteriaBuilder.not(root.get("isDeleted"));

            // Исключаем друзей неподходящие статусы
            Predicate excludeFriends = criteriaBuilder.not(root.get("userId").in(userFriends));
            Predicate excludeStatuses = criteriaBuilder.not(
                    root.get("friendsStatusCode").in(
                            FriendsStatus.BLOCKED.name(),
                            FriendsStatus.DECLINED.name()
                    )
            );
            // Объединяем предикаты
            return criteriaBuilder.and(friendsFriends, notCurrentUser, excludeFriends, excludeStatuses, deletedUsers);
        };
    }

    public Specification<Friend> getFriendshipRequests(UUID currentUserId) {
        return (root, query, criteriaBuilder) -> {
            Predicate user = criteriaBuilder.equal(root.get("userId"), currentUserId);
            Predicate status = criteriaBuilder.equal(root.get("friendsStatusCode"), FriendsStatus.REQUEST_TO.name());

            return criteriaBuilder.and(user, status);
        };
    }

    public Specification<Friend> getFriendsAndSubscribers(UUID userId) {
        return (root, query, criteriaBuilder) -> {
            Predicate user = criteriaBuilder.equal(root.get("userId"), userId);
            Predicate statuses = root.get("friendsStatusCode").in(List.of(FriendsStatus.FRIEND, FriendsStatus.WATCHING));
            return criteriaBuilder.and(user, statuses);
        };
    }
}
