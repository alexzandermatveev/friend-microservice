package com.example.friend_microservice.controller;

import com.example.friend_microservice.dto.FriendShortDto;
import com.example.friend_microservice.service.FriendsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendsController implements ControllerInitial {

    private final FriendsService friendsService;

    @PutMapping("/{id}/approve")
    @Override
    public FriendShortDto proveFriendship(@PathVariable String id) {
        log.warn("Подтверждение дружбы по Id: {}", id);
        return friendsService.approveFriendship(id);
    }

    @PutMapping("/unblock/{id}")
    @Override
    public FriendShortDto unblockFriend(@PathVariable String id) {
        log.warn("Разблокировка по Id: {}", id);
        return friendsService.unblockFriendship(id);
    }

    @PutMapping("/block/{id}")
    @Override
    public FriendShortDto blockFriend(@PathVariable String id) {
        log.warn("Блокировка по Id: {}", id);
        return friendsService.blockFriendship(id);
    }

    @PostMapping("/{id}/request")
    @Override
    public FriendShortDto requestFriendship(@PathVariable String id) {
        log.warn("Запрос дружбы по id: {}", id);
        return friendsService.createFriendRequest(id);
    }

    @PostMapping("/subscribe/{id}")
    @Override
    public FriendShortDto subscribeFriend(@PathVariable String id) {
        log.warn("Оформление подписки по id: {}", id);
        return friendsService.subscribeFriend(id);
    }

    @GetMapping
    @Override
    public Page<FriendShortDto> getAllFriends(@RequestParam(defaultValue = "ALL") String statusCode,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "3") int size) {
        log.warn("Получение всех друзей по статусу: {}", statusCode);
        return friendsService.getFriendsByStatus(statusCode, page, size);
    }

    @GetMapping("/{id}")
    @Override
    public FriendShortDto getRelationshipWithUser(@PathVariable String id) {
        log.warn("Получить отношения с пользователем с id: {}", id);
        return friendsService.getRelationshipWithUser(id);
    }

    @GetMapping("/status/{status}")
    @Override
    public List<UUID> getALlRelationsWithStatus(@PathVariable String status) {
        log.warn("Получить все отношения со статусом: {}", status);
        return friendsService.getAllRelationsWithStatus(status);
    }

    @GetMapping("/recommendations")
    @Override
    public List<FriendShortDto> getRecomendedFriends() {
        log.warn("Получить рекомендации");
        return friendsService.getRecommendations();
    }

    @GetMapping("/friendId")
    @Override
    public List<UUID> getALlRelationsWithUser() {
        log.warn("Получить все отношения с пользователем");
        return friendsService.getAllRelationsWithUser();
    }

    @GetMapping("/friendId/{id}")
    @Override
    public List<UUID> getALlRelationsWithUserById(@PathVariable String id) {
        log.warn("Получить все отношения (ids из БД) с пользователем с id: {}", id);
        return friendsService.getAllRelationsWithUserById(id);
    }

    @GetMapping("/receiverId/{id}")
    public List<UUID> getUserRelationsById(@PathVariable String id){
        log.warn("Получить все id друзей и подписчиков с пользователем по id: {}", id);
        return friendsService.getFriendsAndSubscribersForUserById(id);
    }

    @GetMapping("/count")
    @Override
    public Integer countRequestsForFriendship() {
        log.warn("Получить количество отношений");
        return friendsService.countRequestsForFriendship();
    }

    @GetMapping("/check")
    @Override
    public String getStatusBetweenTwoUsers(@RequestParam List<String> ids) {
        log.warn("Получить отношения между пользователями с id: {}", ids);
        return friendsService.getStatusBetweenTwoUsers(ids);
    }

    @GetMapping("/blockFriendId")
    @Override
    public List<UUID> getALlBlockedUsers() {
        log.warn("Получение списка id пользователей, которые заблокировали пользователя");
        return friendsService.getAllUsersWhoBlocked();
    }

    @DeleteMapping("/{friendId}")
    @Override
    public Boolean dellRelationsWithUserById(@PathVariable String friendId) {
        log.warn("Удаление дружбы по Id: {}", friendId);
        return friendsService.dellRelationsWithUserById(friendId);
    }

    @PutMapping("/markDeleted/{friendId}")
    public List<FriendShortDto> markAsDeleted(@PathVariable String id,
                                              @RequestParam Boolean isDeleted) {
        return friendsService.markAsDeletedAbstract(id, isDeleted);
    }
}
