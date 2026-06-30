package com.example.team8salecommerce.domain.chat.pubsub;

import com.example.team8salecommerce.domain.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub을 끈 환경에서 사용하는 단일 서버용 브로드캐스터입니다.
 *
 * <p>운영에서는 여러 서버에 메시지를 전달해야 하므로 {@link RedisChatMessagePublisher}를 사용합니다.
 * 하지만 테스트나 로컬 개발에서는 Redis 서버를 항상 띄우기 번거로울 수 있습니다.
 * 그럴 때 {@code chat.redis-pubsub.enabled=false}로 설정하면 이 클래스가 대신 사용됩니다.</p>
 *
 * <p>이 방식은 현재 서버에 연결된 구독자에게만 메시지를 보냅니다.
 * 따라서 서버가 2대 이상인 운영 환경의 최종 해결책은 아니고, 단일 서버 환경용 대체 구현입니다.</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.redis-pubsub", name = "enabled", havingValue = "false")
public class LocalChatMessageBroadcaster implements ChatMessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis를 거치지 않고 현재 서버의 STOMP 구독자에게 직접 메시지를 보냅니다.
     */
    @Override
    public void broadcast(ChatMessageResponse message) {
        messagingTemplate.convertAndSend(ChatRedisChannel.stompDestination(message.roomId()), message);
    }
}
