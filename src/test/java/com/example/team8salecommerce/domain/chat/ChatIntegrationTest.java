package com.example.team8salecommerce.domain.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatIntegrationTest {

	@MockitoBean
	private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createChatRoom() throws Exception {
        signup("chat-room@example.com", "Password123!", "chat-room-user");
        String accessToken = loginAndReadAccessToken("chat-room@example.com", "Password123!");

        mockMvc.perform(post("/chat/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "general"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("general"))
                .andExpect(jsonPath("$.data.createdByNickname").value("chat-room-user"));
    }

    @Test
    void createChatRoomRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "general"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void signup(String email, String password, String nickname) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "nickname": "%s"
                                }
                                """.formatted(email, password, nickname)))
                .andExpect(status().isOk());
    }

    private String loginAndReadAccessToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int startIndex = response.indexOf(marker) + marker.length();
        int endIndex = response.indexOf("\"", startIndex);
        return response.substring(startIndex, endIndex);
    }
}
