package com.example.friend_microservice.controller;

import com.example.friend_microservice.dto.FriendShortDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ControllerInitial {
    /**
     * /api/v1/friends/{id}/approve
     * Method PUT
     *
     * @param id
     * @return
     */
    FriendShortDto proveFriendship(String id);

    /**
     * /api/v1/friends/unblock/{id}
     * Method PUT
     */
    FriendShortDto unblockFriend(String id);

    /**
     * /api/v1/friends/block/{id}
     * Method PUT
     *
     * @param id
     * @return
     */
    FriendShortDto blockFriend(String id);

    /**
     * /api/v1/friends/{id}/request
     * Method POST
     *
     * @param id
     * @return
     */
    FriendShortDto requestFriendship(String id);

    /**
     * /api/v1/friends/subscribe/{id}
     * Method POST
     *
     * @param id
     * @return
     */
    FriendShortDto subscribeFriend(String id);

    /**
     * Получите идентификаторы ваших друзей по статусу дружбы между ними.
     * /api/v1/friends
     * Method GET
     * default page = 0, size = 3
     *
     * @return
     */
    Page<FriendShortDto> getAllFriends(String statusCode, int page, int size);

    /**
     * /api/v1/friends/{id}
     * Method GET
     *
     * @param id
     * @return
     */
    FriendShortDto getRelationshipWithUser(String id);

    /**
     * /api/v1/friends/status/{status}
     * Method GET
     */
    List<UUID> getALlRelationsWithStatus(String status);

    /**
     * /api/v1/friends/recommendations
     * Method GET
     * Отдает список 'друзья друзей' пользователя
     */
    List<FriendShortDto> getRecomendedFriends();

    /**
     * /api/v1/friends/friendId
     * Method GET
     */
    List<UUID> getALlRelationsWithUser();

    /**
     * /api/v1/friends/friendId/{id}
     * Method GET
     */
    List<UUID> getALlRelationsWithUserById(String id);

    /**
     * /api/v1/friends/count
     * количество запросов дружбы
     * Method GET
     */
    Integer countRequestsForFriendship();

    /**
     * /api/v1/friends/check
     * Получение текущего статуса отношений двух пользователей
     *
     * @param ids - пара id двух пользователей для определения их отношения
     *            Method GET
     */
    String getStatusBetweenTwoUsers(List<String> ids);

    /**
     * /api/v1/friends/blockFriendId
     * Получение списка id пользователей, которые заблокировали пользователя
     * Method GET
     */
    List<UUID> getALlBlockedUsers();

    /**
     * /api/v1/friends/{friendId}
     * Удаление отношения с пользователем по Id
     * Method DELETE
     */
    Boolean dellRelationsWithUserById(String friendId);


}
