package com.example.friend_microservice.mapping;

import com.example.friend_microservice.dto.FriendShortDto;
import com.example.friend_microservice.entities.Friend;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FriendMapper {

    FriendMapper INSTANCE = Mappers.getMapper(FriendMapper.class);

    @Mapping(source = "friendsStatusCode", target = "statusCode")
    FriendShortDto friendToDto(Friend friend);

}
