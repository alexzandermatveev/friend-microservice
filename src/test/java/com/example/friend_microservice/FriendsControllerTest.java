package com.example.friend_microservice;

import com.example.friend_microservice.dto.FriendShortDto;
import com.example.friend_microservice.repository.FriendsRepository;
import com.example.friend_microservice.securityConfig.JwtFilter;
import com.example.friend_microservice.service.FriendsService;
import jakarta.ws.rs.core.MediaType;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(properties = "spring.profiles.active=test", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//  Запускаем реальный Spring Boot
@AutoConfigureMockMvc //  Включаем MockMvc
@TestPropertySource(locations = "classpath:application-test.yml") //  Используем тестовый application.yml
@ExtendWith(MockitoExtension.class)
class FriendsControllerTest {

    private final String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwYjZmMGQzYy0xMmEzLTQ0ZjctYTgwZi0xMWQ4NzhmNWM2YjYiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjJ9.h_nh7Yuzdajr81aK2WWjDQjwPb_BhWApp0zDqOT27Dg";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendsRepository friendsRepository;

    @MockBean
    private FriendsService friendsService; // Замокировали сервис, чтобы не тестировать его

    @SpyBean
    private JwtFilter jwtFilter; //  Используем SpyBean, чтобы не отключать весь фильтр

    private final String token = "Bearer " + jwt;

    @BeforeEach
    void setUp() {
        // Замокируем только метод валидации JWT (остальной код JwtFilter остается рабочим)
        Mockito.doReturn(true).when(jwtFilter).validateTokenWithAuthService(anyString());
    }

    @Test
    void testGetRelationshipWithUser_Success() throws Exception {
        FriendShortDto mockFriend = new FriendShortDto();
        mockFriend.setId(UUID.randomUUID().toString());
        mockFriend.setFriendId(UUID.randomUUID().toString());
        String friendId = UUID.randomUUID().toString();

        // Мокируем поведение сервиса
        Mockito.when(friendsService.getRelationshipWithUser(anyString()))
                .thenReturn(mockFriend);

        mockMvc.perform(get("/api/v1/friends/{id}", friendId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(), // Ожидаем 200 OK
                        MockMvcResultMatchers.jsonPath("$.id").value(mockFriend.getId())
                );
    }

    @Test
    void testGetRelationshipWithUser_InvalidUUID() throws Exception {
        String invalidId = "invalid-uuid";
        Mockito.when(friendsService.getRelationshipWithUser(invalidId))
                .thenThrow(new IllegalArgumentException("was sent not a UUID value"));

        mockMvc.perform(get("/api/v1/friends/{id}", invalidId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isBadRequest(), // Ожидаем 400
                        MockMvcResultMatchers.content().string(new IllegalArgumentException("was sent not a UUID value").toString())
                );

    }

    @Test
    void testGetRelationshipWithUser_Unauthorized() throws Exception {
        String friendId = UUID.randomUUID().toString();

        // Теперь фильтр вернет false - запрос должен отклониться
        Mockito.doReturn(false).when(jwtFilter).validateTokenWithAuthService(anyString());

        mockMvc.perform(get("/api/v1/friends/{id}", friendId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Должен вернуть 401 Unauthorized
    }

    @Test
    void testRequestFriendship_Success() throws Exception {
        String friendId = UUID.randomUUID().toString();
        FriendShortDto friendShortDto = new FriendShortDto();
        friendShortDto.setId(UUID.randomUUID().toString());
        friendShortDto.setFriendId(UUID.randomUUID().toString());

        Mockito.when(friendsService.createFriendRequest(anyString()))
                .thenReturn(friendShortDto);

        mockMvc.perform(post("/api/v1/friends/{id}/request", friendId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(), // Проверяем 200 ОК
                        MockMvcResultMatchers.jsonPath("$.id").value(friendShortDto.getId()),
                        MockMvcResultMatchers.jsonPath("$.friendId").value(friendShortDto.getFriendId())
                );
    }
}
