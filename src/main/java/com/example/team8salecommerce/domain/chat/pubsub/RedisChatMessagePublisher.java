package com.example.team8salecommerce.domain.chat.pubsub;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 저장된 채팅 메시지를 Redis Pub/Sub 채널로 발행하는 Publisher입니다.
 *
 * <p>Publisher는 "메시지를 Redis로 내보내는 역할"만 합니다.
 * 자기 서버의 WebSocket 구독자에게 직접 보내는 일은 하지 않습니다.
 * 직접 보내 버리면 서버가 여러 대일 때 흐름이 꼬이기 쉽기 때문입니다.</p>
 *
 * <p>다중 서버 흐름은 다음과 같습니다.</p>
 * <ol>
 *     <li>서버 A가 클라이언트에게 채팅 메시지를 받습니다.</li>
 *     <li>서버 A가 메시지를 DB에 저장합니다.</li>
 *     <li>서버 A의 Publisher가 Redis {@code chat-room:{roomId}} 채널에 메시지를 발행합니다.</li>
 *     <li>서버 A, 서버 B, 서버 C의 Subscriber가 Redis 메시지를 받습니다.</li>
 *     <li>각 서버는 자기 서버에 붙어 있는 STOMP 구독자에게 메시지를 전달합니다.</li>
 * </ol>
 *
 * <p>Redis Pub/Sub은 캐시와 다르게 메시지를 저장해 두지 않습니다.
 * Subscriber가 잠깐 내려가 있으면 그동안 발행된 메시지는 놓칠 수 있습니다.
 * 메시지 유실까지 보완해야 한다면 Redis Streams, Kafka 같은 저장형 메시지 브로커를 검토해야 합니다.</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.redis-pubsub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisChatMessagePublisher implements ChatMessageBroadcaster {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 단위 Redis 채널에 메시지를 JSON 문자열로 발행합니다.
     */
    @Override
    public void broadcast(ChatMessageResponse message) {
        try {
            // 채팅방 ID를 기준으로 Redis 채널을 분리합니다. 예: chat-room:10
            String channel = ChatRedisChannel.roomChannel(message.roomId());

            // Redis Pub/Sub은 문자열/바이트 기반으로 메시지를 주고받기 때문에 DTO를 JSON으로 바꿉니다.
            String payload = objectMapper.writeValueAsString(message);

            // 이 한 줄이 Redis Pub/Sub의 "발행"입니다. 같은 채널을 구독 중인 모든 서버가 메시지를 받습니다.
            stringRedisTemplate.convertAndSend(channel, payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("채팅 메시지를 Redis Pub/Sub으로 발행할 수 없습니다.", exception);
        }
    }
}
