package com.example.friend_microservice;


import com.example.friend_microservice.dto.FriendShortDto;
import com.example.friend_microservice.repository.FriendsRepository;
import com.example.friend_microservice.securityConfig.JwtFilter;
import com.example.friend_microservice.service.FriendsService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.UUID;

@SpringBootTest(properties = "spring.profiles.active=test", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//  Запускаем реальный Spring Boot
@AutoConfigureMockMvc //  Включаем MockMvc
@TestPropertySource(locations = "classpath:application-test.yml") //  Используем тестовый application.yml
@ExtendWith(MockitoExtension.class)
public class ServiceTest {
    private final String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwYjZmMGQzYy0xMmEzLTQ0ZjctYTgwZi0xMWQ4NzhmNWM2YjYiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjJ9.h_nh7Yuzdajr81aK2WWjDQjwPb_BhWApp0zDqOT27Dg";

    private final UUID uuid1 = UUID.randomUUID();
    private final UUID uuid2 = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FriendsRepository friendsRepository;

    @Autowired
    private FriendsService friendsService;



    private final String token = "Bearer " + jwt;

    @BeforeEach
    void setup() {

        // Создаем моки для SecurityContext и Authentication
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Authentication authentication = Mockito.mock(Authentication.class);

        // Настраиваем мок, чтобы getPrincipal() возвращал UUID пользователя
        Mockito.when(authentication.getPrincipal()).thenReturn(uuid1.toString());

        // Настраиваем мок, чтобы getAuthentication() возвращал мокнутую аутентификацию
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        // Устанавливаем мокнутый SecurityContext в SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);

        // Очищаем базу данных перед каждым тестом
        friendsRepository.deleteAll();
    }

    @Test
    void testApproveFriendshipWithIllegalArg() {
        // точно не найдет
        Assertions.assertThatThrownBy(() -> friendsService.approveFriendship(UUID.randomUUID().toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Friendship request not found");
    }

    


}
