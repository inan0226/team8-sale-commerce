package com.example.team8salecommerce.domain.chat.pubsub;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class LocalChatMessageBroadcasterTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final LocalChatMessageBroadcaster broadcaster = new LocalChatMessageBroadcaster(messagingTemplate);

    @Test
    void sendsMessageDirectlyToLocalStompSubscribers() {
        ChatMessageResponse response = new ChatMessageResponse(
                1L,
                10L,
                2L,
                "member",
                "hello",
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );

        broadcaster.broadcast(response);

        verify(messagingTemplate).convertAndSend("/sub/chat/rooms/10", response);
    }
}
