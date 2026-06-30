package com.example.team8salecommerce.domain.chat.pubsub;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 채널에서 채팅 메시지를 받아 현재 서버의 STOMP 구독자에게 전달하는 Subscriber입니다.
 *
 * <p>Subscriber는 "Redis에서 메시지를 받아 WebSocket 구독자에게 다시 뿌리는 역할"을 합니다.
 * 서버가 여러 대라면 각 서버마다 이 Subscriber가 떠 있고, 모두 {@code chat-room:*} 패턴을 구독합니다.</p>
 *
 * <p>중요한 점은 Redis에서 받은 메시지를 모든 사용자에게 무조건 보내는 것이 아니라,
 * 해당 채팅방을 구독 중인 클라이언트에게만 보내기 위해 STOMP destination을
 * {@code /sub/chat/rooms/{roomId}}로 맞춘다는 점입니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.redis-pubsub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis에서 메시지를 수신하면 STOMP destination으로 변환해 브로드캐스트합니다.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // Redis가 넘겨준 채널명입니다. 예: chat-room:10
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);

        try {
            // 채널명에서 roomId를 꺼내 STOMP destination을 만들 때 사용합니다.
            Long roomId = ChatRedisChannel.extractRoomId(channel);

            // Publisher가 JSON으로 보낸 payload를 다시 ChatMessageResponse DTO로 복원합니다.
            ChatMessageResponse response = objectMapper.readValue(message.getBody(), ChatMessageResponse.class);

            // 현재 서버에 연결된 클라이언트 중 해당 채팅방을 구독한 사용자에게만 메시지를 보냅니다.
            messagingTemplate.convertAndSend(ChatRedisChannel.stompDestination(roomId), response);
        } catch (IOException | IllegalArgumentException exception) {
            // Pub/Sub은 재전송 보장을 하지 않으므로, 잘못된 메시지는 로그로 남기고 다음 메시지 처리를 이어갑니다.
            log.warn("Redis Pub/Sub 채팅 메시지 처리에 실패했습니다. channel={}", channel, exception);
        }
    }
}
