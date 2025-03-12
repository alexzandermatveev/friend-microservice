package com.example.friend_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FriendShortDto {
    /**
     * ID записи в БД
     */
    private String id;
    /**
     * Текущий статус отношения
     */
    private String statusCode;
    /**
     * ID второго пользователя в отношении
     */
    private String friendId;
    /**
     * Предыдущий статус отношения
     */
    private String previousStatusCode;
    /**
     * Рейтинг пользователя в отношении
     */
    private int rating;
    /**
     * Флаг удаления отношения
     */
    private boolean isDeleted;
}
