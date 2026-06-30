package com.example.team8salecommerce.domain.chat.pubsub;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class ChatMessageSubscriberTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final ChatMessageSubscriber subscriber = new ChatMessageSubscriber(objectMapper, messagingTemplate);

    @Test
    void sendsRedisMessageToLocalStompSubscribers() throws Exception {
        ChatMessageResponse response = new ChatMessageResponse(
                1L,
                10L,
                2L,
                "member",
                "hello",
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );
        Message redisMessage = mock(Message.class);
        when(redisMessage.getChannel()).thenReturn("chat-room:10".getBytes(StandardCharsets.UTF_8));
        when(redisMessage.getBody()).thenReturn(objectMapper.writeValueAsBytes(response));

        subscriber.onMessage(redisMessage, "chat-room:*".getBytes(StandardCharsets.UTF_8));

        verify(messagingTemplate).convertAndSend("/sub/chat/rooms/10", response);
    }
}
