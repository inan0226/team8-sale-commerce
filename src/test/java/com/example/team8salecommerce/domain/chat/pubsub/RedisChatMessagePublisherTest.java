package com.example.team8salecommerce.domain.chat.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisChatMessagePublisherTest {

    private final StringRedisTemplate stringRedisTemplate = org.mockito.Mockito.mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RedisChatMessagePublisher publisher = new RedisChatMessagePublisher(stringRedisTemplate, objectMapper);

    @Test
    void publishesMessageToRoomChannel() throws Exception {
        ChatMessageResponse message = new ChatMessageResponse(
                1L,
                10L,
                2L,
                "member",
                "hello",
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );

        publisher.broadcast(message);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(stringRedisTemplate).convertAndSend(eq("chat-room:10"), payloadCaptor.capture());

        ChatMessageResponse publishedMessage = objectMapper.readValue(payloadCaptor.getValue(), ChatMessageResponse.class);
        assertThat(publishedMessage).isEqualTo(message);
    }
}
