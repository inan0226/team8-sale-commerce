package com.example.team8salecommerce.domain.chat.pubsub;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 채팅 Redis Pub/Sub 구독 컨테이너를 설정합니다.
 *
 * <p>Spring에서 Redis Pub/Sub 메시지를 받으려면 {@link RedisMessageListenerContainer}가 필요합니다.
 * 이 컨테이너가 Redis 연결을 유지하면서 특정 채널에 메시지가 들어왔는지 계속 듣고 있다가,
 * 메시지가 오면 {@link ChatMessageSubscriber}를 호출합니다.</p>
 *
 * <p>{@link PatternTopic}으로 {@code chat-room:*} 패턴을 구독하므로,
 * 채팅방이 새로 생겨도 별도의 서버 재시작 없이 {@code chat-room:{roomId}} 채널 메시지를 받을 수 있습니다.</p>
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "chat.redis-pubsub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatRedisPubSubConfig {

    private final ChatMessageSubscriber chatMessageSubscriber;

    /**
     * Redis Pub/Sub 메시지를 수신할 listener container를 등록합니다.
     */
    @Bean
    public RedisMessageListenerContainer chatRedisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);

        // chat-room:* 패턴으로 모든 채팅방 Redis 채널을 한 번에 구독합니다.
        container.addMessageListener(chatMessageSubscriber, new PatternTopic(ChatRedisChannel.roomChannelPattern()));
        return container;
    }
}
