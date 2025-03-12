package com.example.friend_microservice.entities;


import com.example.friend_microservice.enums.FriendsStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "friends")
//@IdClass(Friend.CustomId.class)
public class Friend {
    @Id
    @GeneratedValue
    private UUID id; // уникальный ID записи в БД

    @Column(name = "user_id", nullable = false)
    private UUID userId; // ID текущего пользователя

    @Column(name = "friend_id", nullable = false)
    private UUID friendId; // ID "друга" или второго пользователя в отношении

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false)
    private FriendsStatus friendsStatusCode; // текущий статус отношения

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status_code")
    private FriendsStatus previousFriendsStatusCode; // предыдущий статус отношения

    @Column(name = "rating")
    private Integer rating; // рейтинг пользователя в отношении

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; // флаг удаления отношения
}
