package com.example.team8salecommerce.domain.chat.pubsub;

/**
 * 채팅 Redis 채널명과 STOMP destination을 한곳에서 관리하는 유틸 클래스입니다.
 *
 * <p>초보자가 헷갈리기 쉬운 부분은 Redis 채널과 STOMP destination이 서로 다르다는 점입니다.</p>
 * <ul>
 *     <li>Redis 채널: 서버끼리 메시지를 공유하는 내부 통로입니다. 예: {@code chat-room:10}</li>
 *     <li>STOMP destination: 브라우저 클라이언트가 구독하는 WebSocket 경로입니다. 예: {@code /sub/chat/rooms/10}</li>
 * </ul>
 *
 * <p>서버 A가 Redis 채널에 메시지를 발행하면, 서버 B도 같은 Redis 채널을 구독하고 있다가 메시지를 받습니다.
 * 그 후 각 서버는 자기 서버에 연결된 클라이언트에게 STOMP destination으로 메시지를 보내 줍니다.</p>
 */
public final class ChatRedisChannel {

    private static final String REDIS_CHANNEL_PREFIX = "chat-room:";
    private static final String STOMP_DESTINATION_PREFIX = "/sub/chat/rooms/";
    private static final String ROOM_ID_PATTERN = "*";

    private ChatRedisChannel() {
    }

    /**
     * 특정 채팅방 메시지를 발행할 Redis 채널명을 만듭니다.
     *
     * <p>채팅방마다 채널을 나누면 10번 방 메시지는 {@code chat-room:10},
     * 20번 방 메시지는 {@code chat-room:20}처럼 분리해서 다룰 수 있습니다.</p>
     */
    public static String roomChannel(Long roomId) {
        return REDIS_CHANNEL_PREFIX + roomId;
    }

    /**
     * 모든 채팅방 Redis 채널을 구독하기 위한 패턴을 반환합니다.
     *
     * <p>{@code chat-room:*}는 {@code chat-room:10}, {@code chat-room:20}처럼
     * 채팅방 ID가 붙은 모든 채널을 한 번에 구독하겠다는 뜻입니다.</p>
     */
    public static String roomChannelPattern() {
        return REDIS_CHANNEL_PREFIX + ROOM_ID_PATTERN;
    }

    /**
     * Redis 채널명에서 채팅방 ID를 꺼냅니다.
     *
     * <p>Subscriber는 Redis에서 {@code chat-room:10} 같은 채널명만 받기 때문에,
     * 다시 STOMP destination을 만들려면 여기서 방 ID를 추출해야 합니다.</p>
     */
    public static Long extractRoomId(String channel) {
        if (channel == null || !channel.startsWith(REDIS_CHANNEL_PREFIX)) {
            throw new IllegalArgumentException("채팅 Redis 채널 형식이 올바르지 않습니다: " + channel);
        }

        return Long.valueOf(channel.substring(REDIS_CHANNEL_PREFIX.length()));
    }

    /**
     * 특정 채팅방의 STOMP 구독 destination을 만듭니다.
     *
     * <p>프론트엔드는 이 경로를 구독하고 있다가 서버가 보내는 실시간 메시지를 받습니다.</p>
     */
    public static String stompDestination(Long roomId) {
        return STOMP_DESTINATION_PREFIX + roomId;
    }
}
