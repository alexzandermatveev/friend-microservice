package com.example.friend_microservice.config;

import com.example.friend_microservice.mapping.FriendMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public FriendMapper friendMapper() {
        return FriendMapper.INSTANCE;
    }
}
