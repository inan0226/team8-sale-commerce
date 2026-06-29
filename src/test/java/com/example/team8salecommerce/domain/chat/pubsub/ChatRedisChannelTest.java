package com.example.team8salecommerce.domain.chat.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatRedisChannelTest {

    @Test
    void createsRoomChannelAndPattern() {
        assertThat(ChatRedisChannel.roomChannel(10L)).isEqualTo("chat-room:10");
        assertThat(ChatRedisChannel.roomChannelPattern()).isEqualTo("chat-room:*");
    }

    @Test
    void extractsRoomIdFromChannel() {
        assertThat(ChatRedisChannel.extractRoomId("chat-room:10")).isEqualTo(10L);
    }

    @Test
    void createsStompDestination() {
        assertThat(ChatRedisChannel.stompDestination(10L)).isEqualTo("/sub/chat/rooms/10");
    }
}
