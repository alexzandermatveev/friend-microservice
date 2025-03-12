package com.example.friend_microservice.service;


import com.example.*;
import com.example.friend_microservice.dto.FriendShortDto;
import com.example.friend_microservice.entities.Friend;
import com.example.friend_microservice.enums.FriendsStatus;
import com.example.friend_microservice.mapping.FriendMapper;
import com.example.friend_microservice.repository.FriendsRepository;
import com.example.friend_microservice.repository.SpecRepo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendsService {
    private final FriendsRepository friendRepository;
    private final FriendMapper friendMapper;
    private final EventKafkaProducer kafkaProducer;
    private final SpecRepo specRepo;

    // получаем id из jwt
    private UUID getCurrentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }

    @Builder
    @AllArgsConstructor
    private static class KafkaParams {
        UUID authorId;
        UUID receiverId;
        UUID eventObjectId;
        EventMethod eventMethod;
        NotificationType notificationType;
        String content;
    }

    private void sendToKafka(KafkaParams params) {

        Event event = Event.builder()
                .authorId(params.authorId) // указываем id автора события (всегда пользователь)
                .receiverId(params.receiverId) // Указываем id получателя если он один, например при отправке сообщения (всегда пользователь)
                .parentObjectId(params.authorId) //(тут всегда автор события) Указываем id объекта к которому относится событие если он есть. Например, комментарий на пост указываем id поста
                .eventObjectId(params.eventObjectId) // указываем id объекта события, например при отправке сообщения id этого сообщения
                .eventMethod(params.eventMethod)
                .content(params.content)
                .notificationType(params.notificationType)
                .serviceName(MicroServiceName.FRIEND)
                .creationTime(OffsetDateTime.now())
                .build();

        kafkaProducer.sendMessage(event);
    }

    public FriendShortDto approveFriendship(String stringId) {
        // Ищем запись (текущий пользователь -> друг)
        UUID id;
        try {
            id = UUID.fromString(stringId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }


        Optional<Friend> friendOpt = friendRepository.findByUserIdAndFriendId(getCurrentUserId(), id);

        //если по id записи в БД
//        Optional<Friend> friendOpt = friendRepository.findById(id);

        Friend friend = friendOpt.orElseThrow(() ->
                new IllegalStateException("Friendship request not found")
        );

        if (friend.getFriendsStatusCode().equals(FriendsStatus.FRIEND) || !friend.getUserId().equals(getCurrentUserId()))
            return friendMapper.friendToDto(friend);

        // Обновляем статус текущей записи
        friend.setPreviousFriendsStatusCode(friend.getFriendsStatusCode());
        friend.setFriendsStatusCode(FriendsStatus.FRIEND);
        friend = friendRepository.save(friend);

        // Проверяем и обновляем обратную запись (друг -> текущий пользователь)
        createOrUpdateReverseFriendship(friend.getFriendId(), friend.getUserId(), FriendsStatus.FRIEND);

        KafkaParams params = KafkaParams.builder()
                .authorId(getCurrentUserId())
                .receiverId(friend.getFriendId())
                .eventObjectId(friend.getId())
                .eventMethod(EventMethod.UPDATE)
                .notificationType(NotificationType.FRIEND_APPROVE)
                .content(String.format("Принят в друзья пользователь %s ", id))
                .build();

        sendToKafka(params);

        return friendMapper.friendToDto(friend);
    }

    private void createOrUpdateReverseFriendship(UUID friendId, UUID currentUserId, FriendsStatus friendsStatus) {
        // Проверяем, существует ли обратная запись
        Optional<Friend> reverseFriendOpt = friendRepository
                .findByUserIdAndFriendId(friendId, currentUserId);

        if (reverseFriendOpt.isPresent()) {
            // Обновляем статус обратной записи
            Friend reverseFriend = reverseFriendOpt.get();
            // игнор, если уже установлен такой статус
            if (reverseFriend.getFriendsStatusCode().equals(friendsStatus)) return;
            reverseFriend.setPreviousFriendsStatusCode(reverseFriend.getFriendsStatusCode());
            reverseFriend.setFriendsStatusCode(friendsStatus);
            reverseFriend = friendRepository.save(reverseFriend);

//            KafkaParams params = KafkaParams.builder()
//                    .authorId(friendId)
//                    .receiverId(reverseFriend.getFriendId())
//                    .eventObjectId(reverseFriend.getId())
//                    .eventMethod(EventMethod.UPDATE)
//                    .notificationType(kafkaNotification)
//                    .content(String.format("Принят в друзья пользователь %s ", currentUserId))
//                    .build();
//
//            sendToKafka(params);
        } else {
            // Создаем новую обратную запись
            Friend reverseFriend = new Friend();
            reverseFriend.setUserId(friendId);
            reverseFriend.setFriendId(currentUserId);
            reverseFriend.setFriendsStatusCode(friendsStatus);
            reverseFriend.setPreviousFriendsStatusCode(FriendsStatus.NONE);
            reverseFriend.setDeleted(false);

//            KafkaParams params = KafkaParams.builder()
//                    .authorId(friendId)
//                    .receiverId(reverseFriend.getFriendId())
//                    .eventObjectId(reverseFriend.getId())
//                    .eventMethod(EventMethod.CREATE)
//                    .notificationType(kafkaNotification)
//                    .content(String.format("Принят в друзья пользователь %s ", currentUserId))
//                    .build();

            friendRepository.save(reverseFriend);
//            sendToKafka(params);
        }
    }


    public FriendShortDto blockFriendship(String stringId) {
        UUID currentUserId = getCurrentUserId();

        UUID friendId;
        try {
            friendId = UUID.fromString(stringId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }

        Friend friend = friendRepository.findByUserIdAndFriendId(currentUserId, friendId)
                .orElseGet(() -> Friend.builder()
                        .userId(currentUserId)
                        .friendId(friendId)
                        .friendsStatusCode(FriendsStatus.NONE) // дальше перезаписывается
                        .previousFriendsStatusCode(FriendsStatus.NONE)
                        .isDeleted(false)
                        .build());
//                .orElseThrow(() -> new IllegalStateException("Friend not found"));

        // если по Id сущности в БД
//        Friend friend = friendRepository.findById(friendId).orElseThrow(() -> new IllegalStateException("Friend not found"));

        if(friend.getFriendsStatusCode().equals(FriendsStatus.BLOCKED) ||
        !friend.getUserId().equals(currentUserId)) return friendMapper.friendToDto(friend);

        friend.setPreviousFriendsStatusCode(friend.getFriendsStatusCode());
        friend.setFriendsStatusCode(FriendsStatus.BLOCKED);
        friendRepository.save(friend);

        KafkaParams params = KafkaParams.builder()
                .authorId(currentUserId)
                .receiverId(friendId)
                .eventObjectId(friend.getId())
                .eventMethod(EventMethod.UPDATE)
                .notificationType(NotificationType.FRIEND_APPROVE)
                .content(String.format("Заблокированы отношения с пользователем %s ", friendId))
                .build();

        sendToKafka(params);

        return friendMapper.friendToDto(friend);
    }

    public FriendShortDto unblockFriendship(String stringId) {
        UUID currentUserId = getCurrentUserId();
        UUID targetId;
        try {
            targetId = UUID.fromString(stringId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }
        Friend friend = friendRepository.findByUserIdAndFriendId(currentUserId, targetId)
                .orElseThrow(() -> new IllegalStateException("Friend not found"));

        //если передается id записи из БД
//        Friend friend = friendRepository.findById(targetId).orElseThrow(() -> new IllegalStateException("Friendship not found"));

        if (!friend.getFriendsStatusCode().equals(FriendsStatus.BLOCKED)) {
            throw new IllegalStateException("Friend is not blocked");
        }

        friend.setPreviousFriendsStatusCode(friend.getFriendsStatusCode());
        friend.setFriendsStatusCode(FriendsStatus.NONE);
        friendRepository.save(friend);

        KafkaParams params = KafkaParams.builder()
                .authorId(currentUserId)
                .receiverId(targetId)
                .eventObjectId(friend.getId())
                .eventMethod(EventMethod.UPDATE)
                .notificationType(NotificationType.FRIEND_UNBLOCKED)
                .content(String.format("Разблокированы отношения с пользователем %s ", targetId))
                .build();

        sendToKafka(params);

        return friendMapper.friendToDto(friend);
    }


    public FriendShortDto createFriendRequest(String stringId) {
        UUID currentUserId = getCurrentUserId();

        UUID friendId;
        try {
            friendId = UUID.fromString(stringId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }

        Optional<Friend> optionalRelationship = friendRepository.findByUserIdAndFriendId(currentUserId, friendId);
        if (optionalRelationship.isPresent()) {
            Friend friendship = optionalRelationship.get();
            if (friendship.getFriendsStatusCode().equals(FriendsStatus.REQUEST_TO))
                return friendMapper.friendToDto(friendship);
            friendship.setPreviousFriendsStatusCode(friendship.getFriendsStatusCode());
            friendship.setFriendsStatusCode(FriendsStatus.REQUEST_TO);

            // обратная связь
            createOrUpdateReverseFriendship(friendId, currentUserId, FriendsStatus.REQUEST_FROM);

            friendRepository.save(friendship);

            KafkaParams params = KafkaParams.builder()
                    .authorId(currentUserId)
                    .receiverId(friendId)
                    .eventObjectId(friendship.getId())
                    .eventMethod(EventMethod.UPDATE)
                    .notificationType(NotificationType.FRIEND_REQUEST)
                    .content(String.format("Запрос в друзья к пользователю %s ", friendId))
                    .build();

            sendToKafka(params);

            return friendMapper.friendToDto(friendship);
        }


        Friend friend = new Friend();
        friend.setUserId(currentUserId);
        friend.setFriendId(friendId);
        friend.setFriendsStatusCode(FriendsStatus.REQUEST_TO);
        friend.setPreviousFriendsStatusCode(FriendsStatus.NONE);
        friend.setDeleted(false);

        // обратная связь
        createOrUpdateReverseFriendship(friendId, currentUserId, FriendsStatus.REQUEST_FROM);

        friend = friendRepository.save(friend);

        KafkaParams params = KafkaParams.builder()
                .authorId(currentUserId)
                .receiverId(friendId)
                .eventObjectId(friend.getId())
                .eventMethod(EventMethod.CREATE)
                .notificationType(NotificationType.FRIEND_REQUEST)
                .content(String.format("Запрос в друзья к пользователю %s ", friendId))
                .build();

        sendToKafka(params);

        return friendMapper.friendToDto(friend);
    }


    public FriendShortDto subscribeFriend(String stringId) {
        UUID currentUserId = getCurrentUserId();

        UUID targetUser;
        try {
            targetUser = UUID.fromString(stringId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }

        Optional<Friend> optionalRelationship = friendRepository.findByUserIdAndFriendId(currentUserId, targetUser);
        if (optionalRelationship.isPresent()) {
            Friend friendship = optionalRelationship.get();
            if (friendship.isDeleted() || friendship.getFriendsStatusCode().equals(FriendsStatus.WATCHING)) {
                return friendMapper.friendToDto(friendship); // возвращаем неизменную сущность дружбы,
                // если один из пользователей удален и вся дружба помечена как deleted
            }

            friendship.setPreviousFriendsStatusCode(friendship.getFriendsStatusCode());
            friendship.setFriendsStatusCode(FriendsStatus.WATCHING);
            friendship = friendRepository.save(friendship);

            //записываем подписчика пользователю
            createOrUpdateReverseFriendship(targetUser, currentUserId, FriendsStatus.SUBSCRIBED);

            //уведомляем пользователя targetUser о нашей подписке
            KafkaParams params = KafkaParams.builder()
                    .authorId(currentUserId)
                    .receiverId(targetUser)
                    .eventObjectId(friendship.getId())
                    .eventMethod(EventMethod.UPDATE)
                    .notificationType(NotificationType.FRIEND_SUBSCRIBE)
                    .content(String.format("Пользователь подписался %s ", currentUserId))
                    .build();

            sendToKafka(params);

            return friendMapper.friendToDto(friendship);
        }

        Friend friendship = Friend.builder()
                .userId(currentUserId)
                .friendId(targetUser)
                .previousFriendsStatusCode(FriendsStatus.NONE)
                .friendsStatusCode(FriendsStatus.WATCHING)
                .isDeleted(false)
                .build();

        //записываем подписчика пользователю
        createOrUpdateReverseFriendship(targetUser, currentUserId, FriendsStatus.SUBSCRIBED);

        friendship = friendRepository.save(friendship);

        //уведомляем пользователя targetUser о нашей подписке
        KafkaParams params = KafkaParams.builder()
                .authorId(currentUserId)
                .receiverId(targetUser)
                .eventObjectId(friendship.getId())
                .eventMethod(EventMethod.UPDATE)
                .notificationType(NotificationType.FRIEND_SUBSCRIBE)
                .content(String.format("Пользователь подписался %s ", currentUserId))
                .build();

        sendToKafka(params);

        return friendMapper.friendToDto(friendship);
    }

    public Page<FriendShortDto> getFriendsByStatus(String stringStatus, int page, int size) {
        UUID currentUserId = getCurrentUserId();
        FriendsStatus status;
        if (stringStatus == null) throw new IllegalArgumentException("wasn`t sent a FriendsStatus value");

        try {
            status = FriendsStatus.valueOf(stringStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a FriendsStatus value");
        }

        Page<Friend> friends = friendRepository.findAll(specRepo.getSpecByUserIdAndFriendStatus(currentUserId, status),
                PageRequest.of(page, size));
        return friends.map(friendMapper::friendToDto);
    }


    public FriendShortDto getRelationshipWithUser(String stringId) {
        UUID currentUserId = getCurrentUserId();
        UUID targetUserId;
        try {
            targetUserId = UUID.fromString(stringId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }
        Friend friend = friendRepository.findByUserIdAndFriendId(currentUserId, targetUserId)
                .orElseThrow(() -> new IllegalStateException("Friend not found"));
        return friendMapper.friendToDto(friend);
    }

    public List<UUID> getAllRelationsWithStatus(String stringStatus) {
        UUID currentUserId = getCurrentUserId();
        FriendsStatus targetStatus;
        try {
            targetStatus = FriendsStatus.valueOf(stringStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a FriendsStatus value");
        }
        List<Friend> friends = friendRepository.findAllByUserIdAndFriendsStatusCode(currentUserId, targetStatus);

        return friends.stream()
                .map(Friend::getFriendId)
                .toList();
    }

    public List<FriendShortDto> getRecommendations() {
        //отдает друзей друзей
        UUID currentUserId = getCurrentUserId();
        List<UUID> userFriends = friendRepository.findAllByUserId(currentUserId)
                .stream()
                .map(Friend::getFriendId)
                .toList(); // друзья пользователя (их UUID)

        // Создаем Specification для поиска друзей друзей
        Specification<Friend> specification = specRepo.getAnotherFriendsByFriends(currentUserId, userFriends);
        return friendRepository.findAll(specification).stream()
                .map(friendMapper::friendToDto)
                .toList();
    }


    public List<UUID> getAllRelationsWithUser() {
        //Получение списка id всех отношений с пользователем
        UUID currentUserId = getCurrentUserId();
        return friendRepository.findAllByUserId(currentUserId).stream()
                .map(Friend::getId)
                .toList();
    }


    public List<UUID> getAllRelationsWithUserById(String stringId) {
        //Получение списка id всех отношений с пользователем по id
        UUID targetUser;
        try {
            targetUser = UUID.fromString(stringId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }
        return friendRepository.findAllByFriendId(targetUser).stream()
                .map(Friend::getId)
                .toList();
    }

    public List<UUID> getFriendsAndSubscribersForUserById(String stringId) {
        //Получение списка id всех отношений с пользователем по id
        UUID targetUser;
        try {
            targetUser = UUID.fromString(stringId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }
        return friendRepository.findAll(specRepo.getFriendsAndSubscribers(targetUser))
                .stream()
                .map(Friend::getFriendId)
                .toList();
    }

    public Integer countRequestsForFriendship() {
//        Получение количества запросов дружбы с пользователем
        UUID currentUserId = getCurrentUserId();
        return Math.toIntExact(friendRepository.count(specRepo.getFriendshipRequests(currentUserId)));
    }


    public String getStatusBetweenTwoUsers(List<String> stringList) {
//        Получение текущего статуса отношений двух пользователей
        List<UUID> ids;
        try {
            ids = stringList.stream().map(UUID::fromString).toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a UUID values");
        }

        if (ids.size() != 2) {
            throw new IllegalArgumentException("there are should be only 2 ids");
        }
        Friend friend = friendRepository.findByUserIdAndFriendId(ids.get(0), ids.get(1))
                .orElseThrow(() -> new IllegalStateException("relationship not found"));

        return friend.getFriendsStatusCode().toString();
    }


    public List<UUID> getAllUsersWhoBlocked() {
        // Получение списка id пользователей, которые заблокировали пользователя
        UUID currentUserId = getCurrentUserId();
        return friendRepository.findAllByFriendIdAndFriendsStatusCode(currentUserId, FriendsStatus.BLOCKED).stream()
                .map(Friend::getUserId)
                .toList();
    }

    public boolean dellRelationsWithUserById(String stringId) {
        // если фронт работает по id сущности
//        UUID entityId;
//        try {
//            entityId = UUID.fromString(stringId);
//        } catch (IllegalArgumentException e) {
//            throw new IllegalArgumentException("was sent not a UUID value");
//        }
//        friendRepository.deleteById(entityId);
//        return true;

//        Удаление отношения с пользователем по Id
        UUID currentUserId = getCurrentUserId();
        UUID friendId;
        try {
            friendId = UUID.fromString(stringId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }
        return friendRepository.deleteByUserIdAndFriendId(currentUserId, friendId) >= 1;
    }

    public List<FriendShortDto> markAsDeletedAbstract(String stringId, Boolean targetCondition) {

        if (targetCondition == null) throw new IllegalArgumentException("No param 'isDeleted'");

        UUID currentUserId = getCurrentUserId();
        UUID friendId;

        try {
            friendId = UUID.fromString(stringId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("was sent not a UUID value");
        }

        Optional<Friend> friendship1 = friendRepository.findByUserIdAndFriendId(currentUserId, friendId);
        // обратная связь
        Optional<Friend> friendship2 = friendRepository.findByUserIdAndFriendId(friendId, currentUserId);
        List<Friend> friendList = new ArrayList<>();
        if (friendship1.isPresent()) {
            Friend friend = friendship1.get();
            friend.setDeleted(targetCondition);
            friendList.add(friend);
        }
        if (friendship2.isPresent()) {
            Friend friend = friendship2.get();
            friend.setDeleted(targetCondition);
            friendList.add(friend);
        }
        if (friendList.isEmpty()) {
            throw new IllegalArgumentException("there is no friendship with these users: " + currentUserId + " and " + friendId);
        }
        friendRepository.saveAll(friendList);
        return friendList.stream()
                .map(friendMapper::friendToDto)
                .toList();
    }


}
